package org.qubership.cloud.framework.contexts.xrequestid;

import org.junit.jupiter.api.*;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.Scope;
import org.qubership.cloud.framework.contexts.strategies.AbstractXRequestIdStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Disabled
class RequestIdCtxLeakTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private XRequestIdContextObject newValue;
    private final String expectInScope = "test";
    private ExecutorService executorService;

    @BeforeEach
    void init() {
        executorService = Executors.newFixedThreadPool(1);
    }

    @AfterEach
    void stop() {
        executorService.shutdownNow();
    }

    @AfterEach
    @BeforeEach
    void cleanUp() {
        ContextManager.clearAll();
        newValue = new XRequestIdContextObject(expectInScope);
        MDC.remove(XRequestIdContextObject.X_REQUEST_ID);

    }

    @Test
    void scopeShouldNotGenerateValuesOutsideOfTheScope() {
        String mdcInScope;
        try (Scope scope =
                     ContextManager.newScope(
                             XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME,
                             newValue
                     )
        ) {
            String ctxInScope = getFromContext();
            mdcInScope = getFromMdc();
            Assertions.assertNotNull(ctxInScope, "value was not propagated into scope"); // passes
            Assertions.assertEquals(mdcInScope, ctxInScope, "MDC and Context values differ"); // passes
        }


/*        String outOfScopeCtxValue = ContextManager.<XRequestIdContextObject>getSafe(XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME)
                .map(XRequestIdContextObject::getRequestId)
                .orElse(null);*/
        String outOfScopeMdc = getFromMdc();
        String ctxV = getFromContext();

        Assertions.assertEquals(outOfScopeMdc,ctxV,"RequestId should be the same");
        assertNotEquals(mdcInScope,outOfScopeMdc,"Leak Detected");
/*
        if (Objects.equals(outOfScopeMdc, outOfScopeCtxValue) && outOfScopeMdc != null) {
            log.warn("MDC and context are the same and are not null, this is not newly generated value: " + outOfScopeMdc);
        }
*/

        Assertions.assertAll(
                () -> Assertions.assertNull(outOfScopeMdc, "MDC value leaked") //<- fails
//                ,() -> Assertions.assertNull(outOfScopeCtxValue, "Ctx value leaked") //<- fails
        );
    }

    private String getFromMdc() {
        return MDC.get(AbstractXRequestIdStrategy.MDC_REQUEST_ID_KEY);
    }

    private String getFromContext() {
        return
                ContextManager.<XRequestIdContextObject>getSafe(XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME)
                        .map(XRequestIdContextObject::getRequestId).orElse(null);
    }

    @Test
    void contextShouldNotLeakOutsideOfExecuteWithContext() throws ExecutionException, InterruptedException {
        String beforeInExecutor = executorService.submit(this::getFromMdc).get();
        Assertions.assertNull(beforeInExecutor, "context was not cleaned up");

        String beforeFromMdc = getFromMdc();
        Assertions.assertNull(beforeFromMdc, "context was not cleaned up");

        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        String afterInit = getFromMdc();
        System.out.println("afterInit: " + afterInit);

        //submit in the scope
        Future<String> submit = executorService
                .submit(() -> ContextManager.executeWithContext(contextSnapshot, this::getFromContext));

        submit.get();
        Assertions.assertNull(getFromMdc(), "Value from MDC should be null as context not initialized");
        getFromContext();
        Assertions.assertNotNull(getFromMdc(), "MDC Should be initialized");

        //submit not in the scope but context value leaked
        String[] nonScopedSubmit = executorService.submit(() -> {
            String fromCtx = getFromContext();
            String fromMdc = getFromMdc();
            Assertions.assertEquals(fromCtx, fromMdc);
            return new String[]{
                    fromMdc,
                    fromCtx
            };
        }).get();

        assertNotEquals(submit.get(), nonScopedSubmit[1], "Leak detected value from context and outside are the same"); ///LEAK

        String nonScopedMdc = nonScopedSubmit[0];
        String nonScopedCtxValue = nonScopedSubmit[1];

        if (nonScopedMdc != null) {
            if (nonScopedMdc.equals(nonScopedCtxValue)) {
                //implicitly if MDC and Context values are the same than context leaked from previous task
                log.warn("Context value leaked to the non scoped task");
            } else if (nonScopedCtxValue == null) {
                log.warn("MDC value leaked to the non scoped task");
            }
        }

        Assertions.assertAll(
                () -> Assertions.assertNull(nonScopedMdc, "MDC value leaked"), //<-- fails
                //it is not just MDC key but also Ctx value leaked
                () -> {
                    Assertions.assertNull(nonScopedCtxValue, "Ctx value was leaked");  //<-- fails
                }
        );
    }
}