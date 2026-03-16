package com.netcracker.cloud.framework.contexts.allowedheaders;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.data.ContextDataResponse;
import com.netcracker.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import com.netcracker.cloud.framework.contexts.helper.ContextPropagationTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class AllowedHeadersPropertyTest extends AbstractContextTestWithProperties {
    public static final String HEADERS_ENV = "headers_allowed";
    public static final String HEADERS_PROPERTY = "headers.allowed";
    private static final String CUSTOM_HEADER = "Custom-header-1";
    public static final String ALLOWED_HEADER = "allowed_header";

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables(HEADERS_ENV, CUSTOM_HEADER);

    @BeforeAll
    static void setup() {
        System.clearProperty(HEADERS_PROPERTY);
        ContextPropagationTestUtils.reinitializeRegistry();
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
