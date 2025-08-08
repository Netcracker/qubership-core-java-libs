package com.netcracker.cloud.contexts.apiversion;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.contexts.IncomingContextDataFactory;
import com.netcracker.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;
import com.netcracker.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import com.netcracker.cloud.framework.contexts.apiversion.ApiVersionProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiVersionProviderApiTest {

    @Test
    void checkApiVersionContextName() {
        assertEquals("Api-Version-Context", ApiVersionProvider.API_VERSION_CONTEXT_NAME);
        assertEquals("Api-Version-Context", new ApiVersionProvider().contextName());
    }

    @Test
    void apiVersionProviderMustHaveDefaultConstructor() {
        AllowedHeadersProvider allowedHeadersProvider = new AllowedHeadersProvider();
        assertNotNull(allowedHeadersProvider);
    }

    @Test
    void apiVersionProvideMethodWithIncomingContextData() {
        ApiVersionProvider apiVersionProvider = new ApiVersionProvider();
        ApiVersionContextObject apiVersionContextObject = apiVersionProvider.provide(IncomingContextDataFactory.getApiVersionIncomingContextData());

        assertEquals("v2", apiVersionContextObject.getVersion());
    }

    @Test
    void apiVersionProvideMethodWithNullableParameter() {
        ApiVersionProvider apiVersionProvider = new ApiVersionProvider();
        ApiVersionContextObject apiVersionContextObject = apiVersionProvider.provide(null);

        assertEquals("v1", apiVersionContextObject.getVersion());
    }

}
