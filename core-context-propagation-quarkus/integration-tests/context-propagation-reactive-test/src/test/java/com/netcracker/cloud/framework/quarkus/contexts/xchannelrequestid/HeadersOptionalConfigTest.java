package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject.X_CHANNEL_REQUEST_ID;

@QuarkusTest
class HeadersOptionalConfigTest {

    @Inject
    HeadersOptionalConfig headersOptionalConfig;

    @Test
    void shouldReadEnableOptionalFromProperty() {
        Optional<List<String>> value = headersOptionalConfig.enableOptional();

        assertTrue(value.isPresent(),
                "quarkus.context.propagation.headers.enable.optional must be present when configured");
        assertEquals(List.of(X_CHANNEL_REQUEST_ID), value.get(),
                "SmallRye must parse the comma-separated value into a list");
    }
}
