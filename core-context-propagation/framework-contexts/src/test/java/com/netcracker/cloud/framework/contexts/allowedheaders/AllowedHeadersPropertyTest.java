package com.netcracker.cloud.framework.contexts.allowedheaders;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.data.ContextDataResponse;
import com.netcracker.cloud.framework.contexts.helper.AbstractContextTestWithProperties;

import java.util.Map;

class AllowedHeadersPropertyTest extends AbstractContextTestWithProperties {
    private static final String CUSTOM_HEADER = "Custom-header-1";
    public static final String ALLOWED_HEADER = "allowed_header";

    static Map<String, String> properties = Map.of("headers.allowed", CUSTOM_HEADER);

    @BeforeAll
    static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    static void tearDown() {
        AbstractContextTestWithProperties.parentCleanup(properties);
    }

    @Test
    void initAllowedHeadersContext() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertNotNull(ContextManager.get(ALLOWED_HEADER));
        AllowedHeadersContextObject allowedHeadersContextObject = ContextManager.get(ALLOWED_HEADER);
        Assertions.assertTrue(allowedHeadersContextObject.getHeaders().containsKey(CUSTOM_HEADER));

        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertNotNull(responseContextData.getResponseHeaders().get(CUSTOM_HEADER));
    }

}
