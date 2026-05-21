package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID;

class HeaderPropagationConfigurationTest {

    @BeforeEach
    void setup() {
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @Test
    void shouldBlockXChannelRequestIdByDefault() {
        Assertions.assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID));
        Assertions.assertTrue(HeaderPropagationConfiguration.isRestricted("x-channel-request-id"));
        Assertions.assertEquals(HeaderPropagationConfiguration.RESTRICTED_HEADERS,
                HeaderPropagationConfiguration.restrictedHeaders());
    }

    @Test
    void shouldNotBlockXChannelRequestIdWhenExempted() {
        System.setProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY, X_CHANNEL_REQUEST_ID);
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID));
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted("x-channel-request-id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.restrictedHeaders().isEmpty());
    }

    @Test
    void shouldApplyExemptionsCaseInsensitively() {
        System.setProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY, "x-channel-request-id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID));
    }

    @Test
    void shouldIgnoreUnknownExemptionEntries() {
        // Names that are not in the restricted list must not change anything.
        System.setProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY, "Custom-Header, X-Request-Id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID),
                "Restricted list must remain unchanged when no entry matches it");
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted("Custom-Header"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted("X-Request-Id"));
    }

    @Test
    void shouldTreatEmptyExemptionPropertyAsNoExemption() {
        System.setProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY, "");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID));
        Assertions.assertEquals(HeaderPropagationConfiguration.RESTRICTED_HEADERS,
                HeaderPropagationConfiguration.restrictedHeaders());
    }

    @Test
    void shouldTreatBlankAndCommaOnlyExemptionPropertyAsNoExemption() {
        System.setProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY, " , ,, ");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isRestricted(X_CHANNEL_REQUEST_ID));
    }

    @Test
    void isRestrictedShouldReturnFalseForNullAndBlank() {
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted(null));
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted(""));
        Assertions.assertFalse(HeaderPropagationConfiguration.isRestricted("   "));
    }
}
