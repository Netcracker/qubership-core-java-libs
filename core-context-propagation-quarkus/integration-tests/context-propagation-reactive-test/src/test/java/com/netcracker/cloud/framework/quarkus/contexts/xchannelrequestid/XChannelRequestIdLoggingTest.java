package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.ContextInitializationStep;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.DeserializedIncomingContextData;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class XChannelRequestIdLoggingTest {

    @ConfigProperty(name = "quarkus.log.console.format")
    String configuredPattern;

    @BeforeEach
    void setup() {
        ContextManager.clearAll();
    }

    @AfterEach
    void teardown() {
        ContextManager.clearAll();
    }

    @Test
    void shouldRenderChannelRequestIdWhenHeaderIsPropagated() {
        RequestContextPropagation.initRequestContext(
                new DeserializedIncomingContextData(
                        Map.of(XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID, "ch-42")),
                ContextInitializationStep.PRE_AUTHENTICATION);

        String formatted = formatWithConfiguredPattern("hello with context");

        assertThat("Logged line must contain the channel request id propagated by the upstream header",
                formatted, containsString("channel_request_id=ch-42"));
        assertThat("Logged line must contain the message",
                formatted, containsString("hello with context"));
    }

    @Test
    void shouldRenderCodeDefaultPlaceholderWhenHeaderIsNotPropagated() {
        RequestContextPropagation.initRequestContext(
                new DeserializedIncomingContextData(Collections.emptyMap()),
                ContextInitializationStep.PRE_AUTHENTICATION);

        String formatted = formatWithConfiguredPattern("hello without context");

        assertThat("Pattern must render the code's default \"-\" placeholder",
                formatted, containsString("[channel_request_id=-]"));
        assertThat("No accidental value must appear in the channel id slot",
                formatted, not(containsString("channel_request_id=ch-")));
        assertThat("Logged line must contain the message",
                formatted, containsString("hello without context"));
    }

    private String formatWithConfiguredPattern(String message) {
        ExtLogRecord logRecord = new ExtLogRecord(Level.INFO, message, getClass().getName());
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();
        if (mdcSnapshot != null) {
            mdcSnapshot.forEach(logRecord::putMdc);
        }
        return new PatternFormatter(configuredPattern).format(logRecord);
    }
}
