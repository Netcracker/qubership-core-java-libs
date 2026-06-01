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
 * <h2>Header propagation</h2>
 * <p>Calls {@code /chain/call} via a real HTTP connection (plain {@link RestTemplate}, no
 * mocks).  That endpoint uses a {@link RestTemplate} with {@code SpringRestTemplateInterceptor}
 * to issue a second HTTP request to {@code /chain/echo}.  The echo endpoint returns the headers
 * it actually received.  Every piece of the framework stack (filter, context manager, strategy,
 * MDC, serializer, interceptor) executes for real.
 *
 * <h2>Log-content validation</h2>
 * <p>Mirrors the unit-level scenario from {@code XChannelRequestIdLogbackTest} but through a
 * real HTTP round-trip.  A {@link ListAppender} is attached to the root Logback logger before
 * each test; after the request returns (synchronously), captured events are formatted with the
 * production Logback pattern and asserted against expected substrings.
 *
 * <p>Because the embedded server runs in the same JVM, log events produced by the server thread
 * are visible to the test appender.  Using a synchronous {@code logback-test.xml} (no
 * {@code AsyncAppender}) guarantees that events are delivered before the HTTP response is read.
 *
 * <h2>Why {@code application.yaml} enables the header</h2>
 * <p>{@code X-Channel-Request-Id} is restricted from outgoing propagation by default
 * (see {@link HeaderPropagationConfiguration#RESTRICTED_HEADERS}).  The property
 * {@code context.propagation.headers.enable.optional=X-Channel-Request-Id} in
 * {@code application.yaml} is read by {@code SpringContextProviderConfiguration.init()}
 * and set as a system property, which lifts the restriction at runtime.
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
     * When the upstream request does not carry the header, the framework defaults
     * {@code channelRequestId} to {@code "-"} (see {@code XChannelRequestIdContextObject}
     * constructor).  The downstream service must still receive this default value — the
     * log slot is never empty.
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
    // Log-content tests — mirror XChannelRequestIdLogbackTest but via real HTTP.
    //
    // The filter populates MDC on the server thread; ChainController logs via
    // SLF4J; the ListAppender captures the ILoggingEvent (which carries the
    // thread-local MDC snapshot); PatternLayout formats it using the production
    // pattern; we assert the rendered string.
    // -------------------------------------------------------------------------

    /**
     * When the request carries {@code X-Channel-Request-Id}, the production Logback pattern
     * must render the header value in the {@code [channel_request_id=...]} slot in log lines
     * from <em>both</em> services in the chain:
     * <ul>
     *     <li>{@code /chain/call} — the "first service" that receives the original request;</li>
     *     <li>{@code /chain/echo} — the "second service" reached via {@code SpringRestTemplateInterceptor}.</li>
     * </ul>
     * Capturing events from both endpoints in one {@link ListAppender} (same JVM) proves that
     * {@code SpringRestTemplateInterceptor} correctly serialized the context into the outgoing
     * request headers and that the downstream filter re-populated MDC from them.
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
     * When the request has no {@code X-Channel-Request-Id}, the strategy defaults to {@code "-"}.
     * Both the first and the second service in the chain must log {@code [channel_request_id=-]}
     * — the slot is never empty, and the default travels through the interceptor just as a real id would.
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

    /**
     * Formats all log events captured by the {@link ListAppender} using the encoder
     * configured in {@code logback-test.xml} and returns them as a single concatenated string.
     */
    private String captureFormattedLogs() {
        return listAppender.list.stream()
                .map(event -> new String(configuredEncoder.encode(event), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }
}
