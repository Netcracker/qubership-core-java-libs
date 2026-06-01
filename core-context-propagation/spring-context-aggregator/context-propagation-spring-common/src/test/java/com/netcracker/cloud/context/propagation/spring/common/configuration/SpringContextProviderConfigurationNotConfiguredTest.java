package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID;

@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        // context.propagation.headers.enable.optional intentionally absent
        "headers.allowed=custom-header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationNotConfiguredTest {

    @Test
    void shouldPublishEmptySystemPropertyAndKeepRestrictedList() {
        assertEquals("", System.getProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY),
                String.format("System property %s must be set to empty when no source configures it",
                        HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY));

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID),
                "Restricted list must apply when no value is configured");
    }
}
