package com.netcracker.cloud.context.propagation.sample.xchannelrequestid;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = XChannelRequestIdPropagationApp.class
)
class XChannelRequestIdStartupStateIT {


    @Test
    void initShouldPublishYamlPropertyToSystemProperties() {
        assertEquals(
                XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME,
                System.getProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY),
                "SpringContextProviderConfiguration.init() must read " +
                HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY +
                " from application.yaml and publish it via System.setProperty(). " +
                "If null — the YAML property was not resolved (wrong key, profile mismatch, or property source ordering)."
        );
    }

    @Test
    void cacheShouldAllowPropagationAfterStartup() {
        assertFalse(
                HeaderPropagationConfiguration.isRestricted(
                        XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME),
                "After startup with application.yaml enabling X-Channel-Request-Id, " +
                "isRestricted() must return false. " +
                "If true — either init() never set the system property, or the cache is stale."
        );

        assertTrue(
                HeaderPropagationConfiguration.restrictedHeaders().isEmpty(),
                "After startup with application.yaml enabling X-Channel-Request-Id, " +
                "the restricted list must be empty."
        );
    }
}
