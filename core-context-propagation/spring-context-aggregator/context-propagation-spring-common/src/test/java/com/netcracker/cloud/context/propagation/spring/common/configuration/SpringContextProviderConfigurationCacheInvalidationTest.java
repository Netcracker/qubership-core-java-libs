package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for SpringContextProviderConfiguration.init() method, which was added to bridge the YAML property to System properties and invalidate the cache.
 * 
 * <p>This test simulates the following timing:
 * <ol>
 *     <li>Pre-populate the cache with the default (X-Channel-Request-Id restricted) <b>before</b> Spring starts;</li>
 *     <li>Let Spring start with {@code context.propagation.headers.enable.optional=X-Channel-Request-Id} in the test property source;</li>
 *     <li>Assert that, after {@code init()}, reads return the fresh state — X-Channel-Request-Id is no longer restricted.</li>
 * </ol>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "context.propagation.headers.enable.optional=X-Channel-Request-Id"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationCacheInvalidationTest {

    private static final String X_CHANNEL_REQUEST_ID =
            XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME;

    static {
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        HeaderPropagationConfiguration.resetCache();

        if (!HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID)) {
            throw new IllegalStateException(
                    "Precondition failed: with no system property, X-Channel-Request-Id must be restricted");
        }
        if (HeaderPropagationConfiguration.restrictedHeaders().size() != 1) {
            throw new IllegalStateException(
                    "Precondition failed: default restricted list must have exactly one entry");
        }
    }

    @AfterAll
    static void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @Test
    void initShouldHavePublishedYamlPropertyToSystemProperties() {
        assertEquals(X_CHANNEL_REQUEST_ID,
                System.getProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY),
                "init() must bridge the YAML property to System.setProperty() — " +
                "if this fails, the test setup itself is broken");
    }

    @Test
    void cacheShouldNotBeStaleAfterInitSetsTheProperty() {
        assertFalse(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID),
                "isRestricted() must return false after init() enabled X-Channel-Request-Id via YAML. " +
                "On old code returns true — cache was built before init() ran and was never invalidated.");

        assertTrue(HeaderPropagationConfiguration.restrictedHeaders().isEmpty(),
                "restrictedHeaders() must be empty after init() enabled X-Channel-Request-Id via YAML. " +
                "On old code still contains it — same stale cache.");
    }

    @Test
    void headerShouldBePresentInOutgoingRequestAfterInitEnablesIt() {
        XChannelRequestIdContextObject ctx = new XChannelRequestIdContextObject("ch-customer-99");
        Map<String, Object> outgoing = new HashMap<>();
        ctx.serialize(outgoing::put);

        assertNotNull(outgoing.get(X_CHANNEL_REQUEST_ID),
                "serialize() must write X-Channel-Request-Id to the outgoing request when the property enables it. " +
                "On old code it does not — stale cache still marks it restricted.");
        assertEquals("ch-customer-99", outgoing.get(X_CHANNEL_REQUEST_ID),
                "The header value must be passed through unchanged.");
    }
}
