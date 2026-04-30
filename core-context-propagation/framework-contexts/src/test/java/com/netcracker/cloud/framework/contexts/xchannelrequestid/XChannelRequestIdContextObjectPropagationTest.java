package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
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
import static com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME;

class XChannelRequestIdContextObjectPropagationTest extends AbstractContextTestWithProperties {
    public static final String X_CHANNEL_REQUEST_ID = "X-Channel-Request-Id";

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
        Assertions.assertNotNull(ContextManager.get(X_CHANNEL_REQUEST_ID));
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(X_CHANNEL_REQUEST_ID);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void testXChannelRequestIdPropagation() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        Assertions.assertNotNull(ContextManager.get(X_CHANNEL_REQUEST_ID));
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(X_CHANNEL_REQUEST_ID);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
        ContextManager.set(X_CHANNEL_REQUEST_ID, xChannelRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertNull(responseContextData.getResponseHeaders().get(X_CHANNEL_REQUEST_ID));
    }

    @Test
    void testXChannelRequestIdPropagationWithResponsePropagatableData() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(X_CHANNEL_REQUEST_ID);
        Assertions.assertNotNull(xChannelRequestIdContextObject.getChannelRequestId());
        ContextManager.set(X_CHANNEL_REQUEST_ID, xChannelRequestIdContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        Assertions.assertEquals("-", responseContextData.getResponseHeaders().get(X_CHANNEL_REQUEST_ID));
    }

    @Test
    void testXChannelRequestIdPropagationIsBlockedWhenConfigured() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, X_CHANNEL_REQUEST_ID);
        HeaderPropagationConfiguration.resetCache();
        try {
            RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
            ContextDataResponse responseContextData = new ContextDataResponse();
            RequestContextPropagation.setResponsePropagatableData(responseContextData);
            Assertions.assertEquals("-", responseContextData.getResponseHeaders().get(X_CHANNEL_REQUEST_ID));

            Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();
            Assertions.assertTrue(serializableContextData.getOrDefault(X_CHANNEL_REQUEST_ID_CONTEXT_NAME, Collections.emptyMap()).isEmpty());
        } finally {
            System.clearProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY);
        }
    }

    @Test
    void testXChannelRequestSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(X_CHANNEL_REQUEST_ID, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(X_CHANNEL_REQUEST_ID_CONTEXT_NAME));
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