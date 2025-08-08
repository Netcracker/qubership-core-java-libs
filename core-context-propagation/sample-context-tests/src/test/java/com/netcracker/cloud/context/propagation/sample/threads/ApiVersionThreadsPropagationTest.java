package com.netcracker.cloud.context.propagation.sample.threads;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import com.netcracker.cloud.headerstracking.filters.context.ApiVersionContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.netcracker.cloud.framework.contexts.apiversion.ApiVersionProvider.API_VERSION_CONTEXT_NAME;

class ApiVersionThreadsPropagationTest extends AbstractThreadTest {
    private static final String API_VERSION_VALUE = "v0";
    private static final String DEFAULT_VALUE = "v1";
    final Runnable runnableWithVersion = () -> assertEquals(API_VERSION_VALUE, ApiVersionContext.get());
    final Runnable runnableWithDefaultVersion = () -> assertEquals(DEFAULT_VALUE, ApiVersionContext.get());

    @Test
    void testPropagationForApiVersion() throws Exception {
        ApiVersionContext.set(API_VERSION_VALUE);
        simpleExecutor.submit(runnableWithVersion).get();
    }

    @Test
    void testDefaultPropagationForApiVersion() throws Exception {
        simpleExecutor.submit(runnableWithDefaultVersion).get();
    }

    @Test
    void childThreadDoesntAffectParentOne() {
        ContextManager.set(API_VERSION_CONTEXT_NAME, new ApiVersionContextObject(API_VERSION_VALUE));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> ContextManager.set(API_VERSION_CONTEXT_NAME, new ApiVersionContextObject("v100")));
        executor.shutdown();
        assertEquals(API_VERSION_VALUE, ((ApiVersionContextObject) ContextManager.get(API_VERSION_CONTEXT_NAME)).getVersion());
    }
}
