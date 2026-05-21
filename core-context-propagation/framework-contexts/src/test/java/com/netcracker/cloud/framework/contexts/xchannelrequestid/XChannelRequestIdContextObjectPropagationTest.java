package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.data.ContextDataResponse;
import com.netcracker.cloud.framework.contexts.data.SimpleIncomingContextData;
import com.netcracker.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import com.netcracker.cloud.headerstracking.filters.context.ChannelRequestIdContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netcracker.cloud.framework.contexts.data.ContextDataRequest.CUSTOM_HEADER;

class XChannelRequestIdContextObjectPropagationTest extends AbstractContextTestWithProperties {

    static Map<String, String> properties = Map.of("headers.allowed", CUSTOM_HEADER);

    @BeforeAll
    static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    static void cleanup() {
        AbstractContextTestWithProperties.parentCleanup(properties);
    }

    @Test
    void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new DefaultContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void testXChannelRequestIdPropagation() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
        ContextManager.set(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, xChannelRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertNull(responseContextData.getResponseHeaders().get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
    }

    @Test
    void testXChannelRequestIdPropagationWithResponsePropagatableData() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
        ContextManager.set(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, xChannelRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        Assertions.assertEquals("-", responseContextData.getResponseHeaders().get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
    }

    @Test
    void testXChannelRequestIdPropagationIsBlockedByRestrictedList() {
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        HeaderPropagationConfiguration.resetCache();

        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        Assertions.assertEquals("-", responseContextData.getResponseHeaders().get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();
        Assertions.assertTrue(serializableContextData.getOrDefault(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, Collections.emptyMap()).isEmpty());
    }

    @Test
    void testXChannelRequestSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
    }

    @Test
    void testXChannelRequestIdContextWrapper() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertNotNull(ChannelRequestIdContext.get());

        ChannelRequestIdContext.set("123");
        Assertions.assertEquals("123", ChannelRequestIdContext.get());

        ChannelRequestIdContext.clear();
        Assertions.assertNotNull(ChannelRequestIdContext.get());
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