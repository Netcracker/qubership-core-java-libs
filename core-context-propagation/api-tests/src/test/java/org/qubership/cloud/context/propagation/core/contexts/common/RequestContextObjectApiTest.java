package org.qubership.cloud.context.propagation.core.contexts.common;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.contexts.IncomingContextDataFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider.REQUEST_CONTEXT_NAME;

public class RequestContextObjectApiTest {
    @Test
    public void testGetRequestApi() {
        Map<String, List<String>> requestHttpHeaders = new HashMap<>() {{
            put("Header-1", Collections.singletonList("Header-1-Value"));
            put("Header-2", Collections.singletonList("Header-2-Value"));
        }};
        RequestContextObject requestContextObject = new RequestContextObject(requestHttpHeaders);
        Map<String, List<String>> requestContextHeaders = requestContextObject.getHttpHeaders(); // API
        assertEquals(requestHttpHeaders.get("Header-1").get(0), requestContextHeaders.get("Header-1".toLowerCase()).get(0));
    }

    @Test
    public void testGetDefaultRequestApi() {
        RequestContextObject requestContextObject = new RequestContextObject((Map<String, ?>) null);
        Map<String, List<String>> defaultHttpHeaders = requestContextObject.getHttpHeaders(); // API
        assertEquals(Collections.emptyMap(), defaultHttpHeaders);
    }

    @Test
    public void testGetRequestContextFromContextManager() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
        RequestContextPropagation.initRequestContext(IncomingContextDataFactory.getRequestIncomingContextData());
        RequestContextObject requestContextObject = ContextManager.get(REQUEST_CONTEXT_NAME); // API
        assertEquals("Header-1-Value", requestContextObject.getHttpHeaders().get("Header-1".toLowerCase()).get(0));
    }
}
