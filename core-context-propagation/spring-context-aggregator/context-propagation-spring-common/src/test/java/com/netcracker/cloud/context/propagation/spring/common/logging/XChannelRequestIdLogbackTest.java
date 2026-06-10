package com.netcracker.cloud.context.propagation.spring.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.netcracker.cloud.context.propagation.core.ContextInitializationStep;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.DeserializedIncomingContextData;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class XChannelRequestIdLogbackTest {

    private static final String PATTERN =
            "[%d{yyyy-MM-dd'T'HH:mm:ss.SSS}] [%-5p] " +
                    "[request_id=%X{requestId:--}] [tenant_id=%X{tenantId:--}] " +
                    "[thread=%thread] [class=%c{1}] " +
                    "[channel_request_id=%X{x_channel_request_id:--}] %m%n";

    private LoggerContext loggerCtx;
    private ByteArrayOutputStream captured;
    private OutputStreamAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setup() {
        ContextManager.clearAll();

        loggerCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        captured = new ByteArrayOutputStream();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerCtx);
        encoder.setPattern(PATTERN);
        encoder.start();

        appender = new OutputStreamAppender<>();
        appender.setContext(loggerCtx);
        appender.setEncoder(encoder);
        appender.setOutputStream(captured);
        appender.start();

        logger = loggerCtx.getLogger(XChannelRequestIdLogbackTest.class);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);
    }

    @AfterEach
    void teardown() {
        if (logger != null) logger.detachAppender(appender);
        if (appender != null) appender.stop();
        MDC.clear();
        ContextManager.clearAll();
    }

    @Test
    void shouldRenderChannelRequestIdWhenHeaderIsPropagated() {
        RequestContextPropagation.initRequestContext(
                new DeserializedIncomingContextData(
                        Map.of(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, "ch-42")),
                ContextInitializationStep.PRE_AUTHENTICATION);

        logger.info("hello with context");

        String out = captured.toString(StandardCharsets.UTF_8);
        assertThat("Logged line must contain the channel request id propagated by the upstream header",
                out, containsString("channel_request_id=ch-42"));
        assertThat("Logged line must contain the message",
                out, containsString("hello with context"));
    }

    @Test
    void shouldRenderCodeDefaultPlaceholderWhenHeaderIsNotPropagated() {
        RequestContextPropagation.initRequestContext(
                new DeserializedIncomingContextData(Collections.emptyMap()),
                ContextInitializationStep.PRE_AUTHENTICATION);

        logger.info("hello without context");

        String out = captured.toString(StandardCharsets.UTF_8);
        assertThat("Logged line must contain the code's default \"-\" placeholder",
                out, containsString("[channel_request_id=-]"));
        assertThat("No leftover value must appear in the channel id slot",
                out, not(containsString("channel_request_id=ch-")));
        assertThat("Logged line must contain the message",
                out, containsString("hello without context"));
    }
}
