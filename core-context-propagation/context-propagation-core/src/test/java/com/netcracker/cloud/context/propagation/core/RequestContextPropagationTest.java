package com.netcracker.cloud.context.propagation.core;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.providers.initsteps.InitializationStepPostAuthn;
import org.qubership.cloud.context.propagation.core.providers.initsteps.InitializationStepPreAuthn;
import org.qubership.cloud.context.propagation.core.providers.requestCount.RequestCountContextObject;
import org.qubership.cloud.context.propagation.core.providers.requestCount.RequestCountProvider;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;

class RequestContextPropagationTest {
    @BeforeEach
    void setUp() {
        ContextManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        ContextManager.clearAll();
    }

    @Test
    void initRequestContext() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest()); // filter
        Assertions.assertEquals(5, ContextManager.getAll().size());
        XVersionContextObject xVersionContextObject = ContextManager.get(XVersionProvider.CONTEXT_NAME);
        Assertions.assertEquals("v2", xVersionContextObject.getxVersion());
    }

    @Test
    void initRequestContextPreAuthn() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest(), ContextInitializationStep.PRE_AUTHENTICATION); // filter
        Assertions.assertEquals(3, ContextManager.getAll().size());
        String contextObject = ContextManager.get(InitializationStepPreAuthn.CONTEXT_NAME);
        Assertions.assertEquals(InitializationStepPreAuthn.CONTEXT_NAME, contextObject);
    }

    @Test
    void initRequestContextPostAuthn() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest(), ContextInitializationStep.POST_AUTHENTICATION); // filter
        Assertions.assertEquals(4, ContextManager.getAll().size());
        String contextObject = ContextManager.get(InitializationStepPostAuthn.CONTEXT_NAME);
        Assertions.assertEquals(InitializationStepPostAuthn.CONTEXT_NAME, contextObject);
    }

    @Test
    void populateResponse() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v0");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertEquals("v0", responseContextData.getResponseHeaders().get(X_VERSION_SERIALIZATION_NAME).get(0));
    }

    @Test
    void getDownstreamHeadersResponse() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v0");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);
        Set<String> downstreamHeaders = RequestContextPropagation.getDownstreamHeaders();
        Assertions.assertEquals(2, downstreamHeaders.size());
    }

    @Test
    void clear() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v0");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject("2"));

        Assertions.assertEquals(3, ContextManager.getAll().size());
        Assertions.assertEquals("2", ContextManager.<RequestCountContextObject>get(RequestCountProvider.CONTEXT_NAME).getxVersion());

        ContextManager.clearAll();
        Assertions.assertEquals("1", ContextManager.<RequestCountContextObject>get(RequestCountProvider.CONTEXT_NAME).getxVersion());
    }

    public static class ContextDataRequest implements IncomingContextData {

        Map<String, Object> requestHeaders = new HashMap<>();

        public ContextDataRequest() {
            requestHeaders.put(X_VERSION_SERIALIZATION_NAME, "v2");
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

    public static class ContextDataResponse implements OutgoingContextData {

        private Map<String, List<String>> responseHeaders = new HashMap<>();


        @Override
        public void set(String name, Object values) {
            responseHeaders.put(name, Collections.singletonList((String) values));
        }

        public Map<String, List<String>> getResponseHeaders() {
            return responseHeaders;
        }
    }

}
