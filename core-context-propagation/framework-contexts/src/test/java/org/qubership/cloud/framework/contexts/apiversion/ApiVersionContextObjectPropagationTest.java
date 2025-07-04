package org.qubership.cloud.framework.contexts.apiversion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.headerstracking.filters.context.ApiVersionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.apiversion.ApiVersionContextObject.SERIALIZED_API_VERSION;
import static org.qubership.cloud.framework.contexts.data.ContextDataRequest.CUSTOM_HEADER;

class ApiVersionContextObjectPropagationTest extends AbstractContextTestWithProperties {
    private static final String API_VERSION_CONTEXT_NAME = "Api-Version-Context";
    private static final String DEFAULT_VERSION = "v1";
    private static final String URL_HEADER = "cloud-core.context-propagation.url";

    static Map<String, String> properties = Map.of("headers.allowed", CUSTOM_HEADER);

    @BeforeAll
    protected static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    protected static void cleanup() {
        AbstractContextTestWithProperties.parentCleanup(properties);
    }

    @Test
    void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new DefaultContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(API_VERSION_CONTEXT_NAME));
        ApiVersionContextObject apiVersionContextObject = ContextManager.get(API_VERSION_CONTEXT_NAME);
        Assertions.assertEquals(DEFAULT_VERSION, apiVersionContextObject.getVersion());
    }

    @Test
    void initApiVersionContext() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(API_VERSION_CONTEXT_NAME));
        ApiVersionContextObject apiVersionContextObject = ContextManager.get(API_VERSION_CONTEXT_NAME);
        Assertions.assertEquals("v2", apiVersionContextObject.getVersion());
    }

    @Test
    void testApiVersionContextWrapper() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertEquals("v2", ApiVersionContext.get());

        ApiVersionContext.set("v3");
        Assertions.assertEquals("v3", ApiVersionContext.get());

        ApiVersionContext.clear();
        Assertions.assertEquals(DEFAULT_VERSION, ApiVersionContext.get());
    }

    @Test
    void testApiVersionSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(API_VERSION_CONTEXT_NAME));
    }

    @Test
    void testApiVersionSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(URL_HEADER, "api/v2/some-test-url"));
        ApiVersionContextObject apiVersionContextObject = new ApiVersionContextObject(contextData);

        Map<String, Object> serializableContextData = apiVersionContextObject.getSerializableContextData();

        Assertions.assertEquals(1, serializableContextData.size());
        Assertions.assertEquals("v2", serializableContextData.get(SERIALIZED_API_VERSION));

        ApiVersionContextObject apiVersionContextObject1 = new ApiVersionContextObject(new SimpleIncomingContextData());
        Assertions.assertEquals("v1", apiVersionContextObject1.getSerializableContextData().get(SERIALIZED_API_VERSION));
    }

    public static class DefaultContextDataRequest implements IncomingContextData {

        Map<String, Object> requestHeaders = new HashMap<>();

        public DefaultContextDataRequest() {
            requestHeaders.put("Custom-Header", "value");
        }

        @Override
        public Object get(String name) {
            return requestHeaders.get(name);
        }

        @Override
        public Map<String, List<?>> getAll() {
            return null;
        }

    }

}