package org.qubership.cloud.context.propagation.sample.threads;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import org.qubership.cloud.headerstracking.filters.context.ApiVersionContext;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.qubership.cloud.framework.contexts.apiversion.ApiVersionProvider.API_VERSION_CONTEXT_NAME;
import static org.junit.Assert.*;

public class ApiVersionThreadsPropagationTest extends AbstractThreadTest {
    private static final String API_VERSION_VALUE = "v0";
    private static final String DEFAULT_VALUE = "v1";
    final Runnable runnableWithVersion = () -> assertEquals(API_VERSION_VALUE, ApiVersionContext.get());
    final Runnable runnableWithDefaultVersion = () -> assertEquals(DEFAULT_VALUE, ApiVersionContext.get());

    @Test
    public void testPropagationForApiVersion() throws Exception {
        ApiVersionContext.set(API_VERSION_VALUE);
        simpleExecutor.submit(runnableWithVersion).get();
    }

    @Test
    public void testDefaultPropagationForApiVersion() throws Exception {
        simpleExecutor.submit(runnableWithDefaultVersion).get();
    }

    @Test
    public void childThreadDoesntAffectParentOne() {
        ContextManager.set(API_VERSION_CONTEXT_NAME, new ApiVersionContextObject(API_VERSION_VALUE));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ContextManager.set(API_VERSION_CONTEXT_NAME, new ApiVersionContextObject("v100"));
        });
        executor.shutdown();
        assertEquals(API_VERSION_VALUE, ((ApiVersionContextObject) ContextManager.get(API_VERSION_CONTEXT_NAME)).getVersion());
    }
}
