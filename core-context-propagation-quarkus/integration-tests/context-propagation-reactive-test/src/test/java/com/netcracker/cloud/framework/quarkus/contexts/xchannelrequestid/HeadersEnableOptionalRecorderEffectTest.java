package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID;

/**
 * End-to-end integration scenario in a real running Quarkus application:
 *
 * <ol>
 *     <li>{@code application.properties} declares
 *         {@code quarkus.context.propagation.headers.enable.optional=X-Channel-Request-Id}.</li>
 *     <li>At {@code RUNTIME_INIT}, {@code HeadersOptionalRecorder} reads the config from Arc
 *         and writes {@code context.propagation.headers.enable.optional=X-Channel-Request-Id} into the
 *         JVM system properties.</li>
 *     <li>The non-Quarkus {@link HeaderPropagationConfiguration} reads the system property,
 *         drops {@code X-Channel-Request-Id} from the restricted list, and reports an empty
 *         effective restricted list.</li>
 * </ol>
 *
 */
@QuarkusTest
class HeadersEnableOptionalRecorderEffectTest {

    @Test
    void shouldExposeEnableOptionalValueAsSystemProperty() {
        assertEquals(X_CHANNEL_REQUEST_ID,
                System.getProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY),
                String.format("Recorder must propagate the quarkus.%1$s value " +
                        "to the %1$s system property.", HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY));
    }

    @Test
    void shouldDropEnabledHeaderFromRestrictedList() {
        HeaderPropagationConfiguration.resetCache();

        assertTrue(HeaderPropagationConfiguration.restrictedHeaders().isEmpty(),
                "Any entry of the restricted list must be dropped " +
                        "by the optional-enable configured in application.properties.");
        assertFalse(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID),
                "X-Channel-Request-Id must not be restricted when explicitly enabled.");
    }
}
