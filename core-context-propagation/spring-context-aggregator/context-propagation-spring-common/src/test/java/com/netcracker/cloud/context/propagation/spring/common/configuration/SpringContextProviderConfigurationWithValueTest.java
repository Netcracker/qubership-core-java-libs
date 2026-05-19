package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "context.propagation.headers.enable.optional=X-Channel-Request-Id"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationWithValueTest {

    @Test
    void shouldExemptListedHeaderFromRestrictedList() {
        assertEquals(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, System.getProperty("context.propagation.headers.enable.optional"));

        HeaderPropagationConfiguration.resetCache();
        assertFalse(HeaderPropagationConfiguration.isRestricted(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME),
                "Exempted header must not be blocked");
        assertTrue(HeaderPropagationConfiguration.restrictedHeaders().isEmpty(),
                "Any entry of the restricted list must be removed");
    }
}
