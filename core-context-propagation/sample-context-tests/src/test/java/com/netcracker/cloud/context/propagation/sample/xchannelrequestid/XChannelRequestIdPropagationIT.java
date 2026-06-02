package com.netcracker.cloud.context.propagation.sample.xchannelrequestid;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.read.ListAppender;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end integration test for {@code X-Channel-Request-Id} propagation and MDC logging.
 *
 * <p>Calls {@code /chain/call} via a real HTTP connection; that endpoint forwards to
 * {@code /chain/echo} via {@code SpringRestTemplateInterceptor}. The echo endpoint returns
 * the headers it received, allowing propagation to be asserted end-to-end.
 *
 * <p>Log assertions capture events via a {@link ListAppender} attached to the root Logback
 * logger. The embedded server runs in the same JVM, so server-thread events are visible
 * to the test appender synchronously.
 *
 * <p>{@code X-Channel-Request-Id} propagation is enabled via
 * {@code context.propagation.headers.enable.optional} in {@code application.yaml}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = XChannelRequestIdPropagationApp.class
)
class XChannelRequestIdPropagationIT {

    private static final String X_CHANNEL_REQUEST_ID = "x-channel-request-id";
    private static final String TEST_CHANNEL_ID = "ch-integration-test-99";

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private ListAppender<ILoggingEvent> listAppender;
    private PatternLayoutEncoder configuredEncoder;
    private Logger rootLogger;

    @BeforeEach
    void setup() {
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();

        LoggerContext loggerCtx = (LoggerContext) LoggerFactory.getILoggerFactory();

        rootLogger = loggerCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> stdout =
                (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender("STDOUT");
        configuredEncoder = (PatternLayoutEncoder) stdout.getEncoder();

        listAppender = new ListAppender<>();
        listAppender.setContext(loggerCtx);
        listAppender.start();

        rootLogger.addAppender(listAppender);
    }

    @AfterEach
    void teardown() {
        if (rootLogger != null) rootLogger.detachAppender(listAppender);
        if (listAppender != null) listAppender.stop();
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();
    }

    // -------------------------------------------------------------------------
    // Header propagation tests
    // -------------------------------------------------------------------------

    /**
     * sunnyday: the upstream request carries the header; the downstream service
     * must receive exactly the same value.
     */
    @Test
    void xChannelRequestIdShouldBePropagatedToDownstreamService() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Channel-Request-Id", TEST_CHANNEL_ID);

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "http://localhost:" + port + "/chain/call",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String downstream = response.getBody().get(X_CHANNEL_REQUEST_ID);
        assertNotNull(downstream,
                "X-Channel-Request-Id was NOT present in the downstream request. " +
                "Headers that /chain/echo received: " + response.getBody());
        assertEquals(TEST_CHANNEL_ID, downstream,
                "X-Channel-Request-Id value was altered during propagation");
    }

    /**
     * When the header is absent the framework defaults to {@code "-"}; the downstream
     * service must receive this placeholder value.
     */
    @Test
    void xChannelRequestIdDefaultPlaceholderShouldReachDownstreamServiceWhenHeaderAbsent() {
        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "http://localhost:" + port + "/chain/call",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String downstream = response.getBody().get(X_CHANNEL_REQUEST_ID);
        assertNotNull(downstream,
                "X-Channel-Request-Id was not present at all in the downstream request — " +
                "even the default \"-\" placeholder was not propagated. " +
                "Headers that /chain/echo received: " + response.getBody());
        assertEquals("-", downstream,
                "Expected default placeholder \"-\" but downstream received: " + downstream);
    }

    // -------------------------------------------------------------------------
    // MDC / log-content tests
    // -------------------------------------------------------------------------

    /**
     * When the header is present, both {@code /chain/call} and {@code /chain/echo} log lines
     * must contain {@code [channel_request_id=<value>]}, confirming MDC is populated in
     * both services.
     */
    @Test
    void xChannelRequestIdShouldAppearInLogWhenHeaderProvided() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Channel-Request-Id", TEST_CHANNEL_ID);

        restTemplate.exchange(
                "http://localhost:" + port + "/chain/call",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        String logs = captureFormattedLogs();
        assertThat("Both /chain/call and /chain/echo logs must contain the propagated channel request id",
                logs, containsString("[channel_request_id=" + TEST_CHANNEL_ID + "]"));
        assertThat("No log line must contain the default placeholder when a real id was provided",
                logs, not(containsString("[channel_request_id=-]")));
    }

    /**
     * When the header is absent, both services must log {@code [channel_request_id=-]};
     * the default placeholder travels through the interceptor like a real value.
     */
    @Test
    void xChannelRequestIdDefaultPlaceholderShouldAppearInLogWhenHeaderAbsent() {
        restTemplate.exchange(
                "http://localhost:" + port + "/chain/call",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        String logs = captureFormattedLogs();
        assertThat("Both /chain/call and /chain/echo logs must contain the default \"-\" placeholder",
                logs, containsString("[channel_request_id=-]"));
        assertThat("No log line must contain any real channel id",
                logs, not(containsString("[channel_request_id=ch-")));
    }

    /** Returns all captured log events formatted with the encoder from {@code logback-test.xml}. */
    private String captureFormattedLogs() {
        return listAppender.list.stream()
                .map(event -> new String(configuredEncoder.encode(event), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }
}
