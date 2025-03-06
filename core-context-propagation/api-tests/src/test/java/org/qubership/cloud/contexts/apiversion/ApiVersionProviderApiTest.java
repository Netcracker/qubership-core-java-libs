package org.qubership.cloud.contexts.apiversion;

import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;
import org.qubership.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import org.qubership.cloud.framework.contexts.apiversion.ApiVersionProvider;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiVersionProviderApiTest {

    @Test
    public void checkApiVersionContextName() {
        assertEquals("Api-Version-Context", ApiVersionProvider.API_VERSION_CONTEXT_NAME);
        assertEquals("Api-Version-Context", new ApiVersionProvider().contextName());
    }

    @Test
    public void apiVersionProviderMustHaveDefaultConstructor() {
        AllowedHeadersProvider allowedHeadersProvider = new AllowedHeadersProvider();
        Assert.assertNotNull(allowedHeadersProvider);
    }

    @Test
    public void apiVersionProvideMethodWithIncomingContextData() {
        ApiVersionProvider apiVersionProvider = new ApiVersionProvider();
        ApiVersionContextObject apiVersionContextObject = apiVersionProvider.provide(IncomingContextDataFactory.getApiVersionIncomingContextData());

        assertEquals("v2", apiVersionContextObject.getVersion());
    }

    @Test
    public void apiVersionProvideMethodWithNullableParameter() {
        ApiVersionProvider apiVersionProvider = new ApiVersionProvider();
        ApiVersionContextObject apiVersionContextObject = apiVersionProvider.provide(null);

        assertEquals("v1", apiVersionContextObject.getVersion());
    }

}
