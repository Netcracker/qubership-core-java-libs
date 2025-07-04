package org.qubership.cloud.context.propagation.sample.threads;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject.X_REQUEST_ID;

public class RequestIdThreadsPropagationTest extends AbstractThreadTest {
    private static final String REQUEST_ID = "some-id";
    final Runnable runnableWithRequestId = () -> assertEquals(REQUEST_ID, RequestIdContext.get());
    final Runnable runnableWithoutRequestId = () ->  {
        assertNotNull(RequestIdContext.get());
        assertNotEquals(REQUEST_ID, RequestIdContext.get());
    };

    @Test
    public void testPropagationForRequestId() throws Exception {
        RequestIdContext.set(REQUEST_ID);
        simpleExecutor.submit(runnableWithRequestId).get();
    }

    @Test
    public void testNoPropagationForRequestId() throws Exception {
        simpleExecutor.submit(runnableWithoutRequestId).get();
    }

    @Test
    public void childThreadDoesntAffectParentOne() {
        ContextManager.set(X_REQUEST_ID, new XRequestIdContextObject(REQUEST_ID));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> ContextManager.set(X_REQUEST_ID, new XRequestIdContextObject("new-id")));
        executor.shutdown();
        assertEquals(REQUEST_ID, ((XRequestIdContextObject) ContextManager.get(X_REQUEST_ID)).getRequestId());
    }
}
