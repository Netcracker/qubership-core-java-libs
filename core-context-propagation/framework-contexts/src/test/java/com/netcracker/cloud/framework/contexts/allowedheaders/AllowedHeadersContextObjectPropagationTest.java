package com.netcracker.cloud.framework.contexts.allowedheaders;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.qubership.cloud.framework.contexts.data.ContextDataResponse;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.headerstracking.filters.context.AllowedHeadersContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AllowedHeadersContextObjectPropagationTest extends AbstractContextTestWithProperties {
    public static final String ALLOWED_HEADER = "allowed_header";
    private static final String CUSTOM_HEADER = "Custom-header-1";
    // set lowered `Custom-Header-1` to check case-insensitivity
    static Map<String, String> properties = Map.of("headers.allowed", "custom-header-1");

    @BeforeAll
    protected static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    protected static void cleanup() {
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

    @Test
    void initAllowedHeadersContextWithSpringCommon() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        AllowedHeadersContextObject allowedHeadersContextObject = ContextManager.get(ALLOWED_HEADER);
        Assertions.assertTrue(allowedHeadersContextObject.getHeaders().containsKey(CUSTOM_HEADER));

        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        Assertions.assertNotNull(responseContextData.getResponseHeaders().get(CUSTOM_HEADER));
    }

    @Test
    void testAllowedHeadersContextWrapper() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Map<String, String> testHeaders = new HashMap<>();
        testHeaders.put(CUSTOM_HEADER, "custom_value");
        Assertions.assertEquals(testHeaders, AllowedHeadersContext.getHeaders());

        Map<String, String> otherTestHeaders = new HashMap<>();
        otherTestHeaders.put("Second_header", "Second_value");
        AllowedHeadersContext.set(otherTestHeaders);
        Assertions.assertEquals(otherTestHeaders, AllowedHeadersContext.getHeaders());

        AllowedHeadersContext.clear();
        Assertions.assertTrue(AllowedHeadersContext.getHeaders().isEmpty());
    }

    @Test
    void testAllowedHeadersSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(AllowedHeadersProvider.ALLOWED_HEADER));
    }

    @Test
    void testAllowedHeadersSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(CUSTOM_HEADER, "custom_value"));
        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(contextData, Collections.singletonList(CUSTOM_HEADER));

        Map<String, Object> serializableContextData = allowedHeadersContextObject.getSerializableContextData();

        Assertions.assertEquals(1, serializableContextData.size());
        Assertions.assertEquals("custom_value", serializableContextData.get(CUSTOM_HEADER));

        AllowedHeadersContextObject allowedHeadersContextObject2 = new AllowedHeadersContextObject(new SimpleIncomingContextData(), Collections.singletonList(CUSTOM_HEADER));
        Assertions.assertEquals(0, allowedHeadersContextObject2.getSerializableContextData().size());
    }

}
