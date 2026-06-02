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

/**
 * Spring integration scenario: {@code context.propagation.headers.enable.optional} contains only
 * header names that are not part of the framework's restricted list. The configuration has no
 * effect — the restricted list applies unchanged.
 */
@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "context.propagation.headers.enable.optional=Custom-Header, X-Some-Other-Header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationUnknownEntryTest {

    @Test
    void shouldLeaveRestrictedListIntactWhenEntriesDontMatch() {
        assertEquals("Custom-Header, X-Some-Other-Header",
                System.getProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY));

        assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID),
                "Restricted list must remain intact when no entry matches it");
        assertEquals(HeaderPropagationConfiguration.RESTRICTED_HEADERS,
                HeaderPropagationConfiguration.restrictedHeaders());
    }
}
