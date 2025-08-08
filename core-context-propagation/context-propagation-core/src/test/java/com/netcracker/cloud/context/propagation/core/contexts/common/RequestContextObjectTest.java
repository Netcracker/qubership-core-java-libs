package com.netcracker.cloud.context.propagation.core.contexts.common;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider.REQUEST_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestContextObjectTest {
    private final String headerName = "X-Custom-Header";
    private final String headerValue = "Custom-header-1";

    @BeforeEach
    void setup() {
        RequestContextPropagation.initRequestContext(new IncomingContextDataImpl());
    }

    @Test
    void testRequestWithHeader() {
        assertEquals("Custom-header-1", ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getFirst("X-Custom-Header"));
        ContextManager.set(REQUEST_CONTEXT_NAME, new RequestContextObject(new HashMap<String, List<String>>() {{
            put("X-Custom-Header", Collections.singletonList("Custom-header-2"));
        }}));
        assertEquals("Custom-header-2", ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getFirst("X-Custom-Header"));
    }

    @Test
    void testClearContext() {
        assertNotNull(ContextManager.get(REQUEST_CONTEXT_NAME));
        ContextManager.clear(REQUEST_CONTEXT_NAME);
        assertEquals(0, ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getHttpHeaders().size());
    }

    @Test
    void testGetRequestHeaderByUpperCaseHeaderName() {
        assertEquals(headerValue, ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getHttpHeader("X-CUSTOM-HEADER").get(0));
    }

    @Test
    void testGetRequestHeaderByLowerCaseHeaderName() {
        assertEquals(headerValue, ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getHttpHeader("x-custom-header").get(0));
    }

    @Test
    void testGetFirstByUpperCaseHeaderName() {
        assertEquals(headerValue, ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getFirst("X-CUSTOM-HEADER"));
    }

    @Test
    void testGetFirstByLowerCaseHeaderName() {
        assertEquals(headerValue, ((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getFirst("x-custom-header"));
    }

    @Test
    void getDefaultValue() {
        assertTrue(((RequestContextObject) ContextManager.get(REQUEST_CONTEXT_NAME)).getDefault().isEmpty());
    }

    @Test
    void testFilterByNullHeaderValues() {
        RequestContextObject requestContextObject = new RequestContextObject(Collections.singletonMap(headerName, null));
        assertEquals(0, requestContextObject.getHttpHeader(headerName).size());
    }

    class IncomingContextDataImpl implements IncomingContextData {
        private static final String REQUEST = REQUEST_CONTEXT_NAME;
        final Map<String, Object> contextDataMap = new HashMap<>();

        public IncomingContextDataImpl() {
            contextDataMap.put(REQUEST, new HashMap<String, List<String>>() {{
                put(headerName, Collections.singletonList(headerValue));
            }});
        }

        @Override
        public Object get(String name) {
            return contextDataMap.get(name);
        }

        @Override
        public Map<String, List<?>> getAll() {
            return (Map<String, List<?>>) contextDataMap.get(REQUEST);
        }
    }
}
