package org.qubership.cloud.framework.contexts.xrequestid;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.qubership.cloud.framework.contexts.data.ContextDataResponse;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.data.ContextDataRequest.CUSTOM_HEADER;
import static org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME;

class XRequestIdContextObjectPropagationTest extends AbstractContextTestWithProperties {
    public static final String X_REQUEST_ID = "X-Request-Id";

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
    public void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new DefaultContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(X_REQUEST_ID));
        XRequestIdContextObject xRequestIdContextObject = ContextManager.get(X_REQUEST_ID);
        Assertions.assertNotNull(xRequestIdContextObject.getRequestId());
    }

    @Test
    public void testXRequestIdPropagation() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(X_REQUEST_ID));
        XRequestIdContextObject xRequestIdContextObject = ContextManager.get(X_REQUEST_ID);
        Assertions.assertNotNull(xRequestIdContextObject.getRequestId());
        String testId = xRequestIdContextObject.getRequestId();
        ContextManager.set(X_REQUEST_ID, xRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertEquals(testId, responseContextData.getResponseHeaders().get(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdPropagationWithResponsePropagatableData() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        XRequestIdContextObject xRequestIdContextObject = ContextManager.get(X_REQUEST_ID);
        Assertions.assertNotNull(xRequestIdContextObject.getRequestId());
        String testId = xRequestIdContextObject.getRequestId();
        ContextManager.set(X_REQUEST_ID, xRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        Assertions.assertEquals(testId, responseContextData.getResponseHeaders().get(X_REQUEST_ID));
    }

    @Test
    public void testXRequestIdContextWrapper() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertNotNull(RequestIdContext.get());

        RequestIdContext.set("123");
        Assertions.assertEquals("123", RequestIdContext.get());

        RequestIdContext.clear();
        Assertions.assertNotNull(RequestIdContext.get());
    }

    @Test
    public void testXRequestSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(X_REQUEST_ID, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(X_REQUEST_ID_CONTEXT_NAME));
    }

    @Test
    public void testXRequestSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(X_REQUEST_ID, "12345"));
        XRequestIdContextObject xRequestIdContextObject = new XRequestIdContextObject(contextData);

        Map<String, Object> serializableContextData = xRequestIdContextObject.getSerializableContextData();

        Assertions.assertEquals(1, serializableContextData.size());
        Assertions.assertEquals("12345", serializableContextData.get(X_REQUEST_ID));

        XRequestIdContextObject xRequestIdContextObject1 = new XRequestIdContextObject(new SimpleIncomingContextData());
        Assertions.assertEquals(1, xRequestIdContextObject1.getSerializableContextData().size());
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