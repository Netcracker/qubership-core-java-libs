package com.netcracker.cloud.context.propagation.quarkus.runtime.microprofile.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.executors.ContextAwareExecutorService;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionProvider;
import com.netcracker.cloud.headerstracking.filters.context.RequestIdContext;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@QuarkusTest
public class MicroprofileWithMutinyContextPropagationIT {
    private static final Logger log = Logger.getLogger(MicroprofileWithMutinyContextPropagationIT.class);

    private static final Map<String, Object> contextData = Map.of(
            XRequestIdContextObject.X_REQUEST_ID, "x-request-id-test-val",
            XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "x-version-test-val"
    );

    private RequestContextData requestContextData;
    private ContextHolder upstreamThreadContextHolder;

    @BeforeEach
    public void init() {
        ContextManager.clearAll();

        log.infof("init context data and upstream holder, thread=%s", Thread.currentThread().getName());
        requestContextData = new RequestContextData(contextData);
        RequestContextPropagation.initRequestContext(requestContextData);

        upstreamThreadContextHolder = new ContextHolder();
        upstreamThreadContextHolder.fillValuesFromContexts();
    }

    @Test
    public void contextPropagatesAndStaysTheSame() {
        log.infof("on fork join pool, thread=%s", Thread.currentThread().getName());
        ContextHolder firstDownstreamContextHolder = getContextHolderOnUni(c -> {});
        Assertions.assertEquals(upstreamThreadContextHolder, firstDownstreamContextHolder);
        ContextHolder secondDownstreamContextHolder = getContextHolderOnUni(c -> {});
        Assertions.assertEquals(firstDownstreamContextHolder, secondDownstreamContextHolder);

        ContextHolder contextHolderAfterPropagation = new ContextHolder();
        contextHolderAfterPropagation.fillValuesFromContexts();

        Assertions.assertEquals(upstreamThreadContextHolder, contextHolderAfterPropagation);
    }

    @Test
    public void contextHaveItsOwnData() {
        log.infof("on fork join pool, thread=%s", Thread.currentThread().getName());

        Uni<ContextHolder> uniDownstreamContextHolder = getContextHolderUni(c -> {
            String prevRequestIdVal = c.requestIdFromContext;
            RequestIdContext.set("new-request-id");
            ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject(new RequestContextData(Collections.singletonMap(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "x-version-new-val"))));
            c.fillValuesFromContexts();
            log.infof("changed request id from %s to %s, and xversion from %s to %s", prevRequestIdVal, c.requestIdFromContext);
        });

        ContextHolder downstreamContextHolder = uniDownstreamContextHolder.await().indefinitely();

        Assertions.assertNotEquals(upstreamThreadContextHolder, downstreamContextHolder);

        ContextHolder contextHolderAfterPropagation = new ContextHolder();
        contextHolderAfterPropagation.fillValuesFromContexts();

        Assertions.assertEquals(upstreamThreadContextHolder, contextHolderAfterPropagation);
        Assertions.assertNotEquals(downstreamContextHolder, contextHolderAfterPropagation);
    }

    @Test
    public void contextPropagatesAndSubscriptionContextHasCallerThreadContextValue() {
        ExecutorService ctxAwareExecutorService = new ContextAwareExecutorService(Executors.newSingleThreadExecutor());
        log.infof("on core context-aware executor, thread=%s", Thread.currentThread().getName());

        performAsyncThenSubscribeOnResultContext(ctxAwareExecutorService, c -> {
            String prevRequestIdVal = c.requestIdFromContext;
            RequestIdContext.set("new-request-id");
            ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject(new RequestContextData(Collections.singletonMap(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "x-version-new-val"))));
            c.fillValuesFromContexts();
            log.infof("changed request id from %s to %s, and xversion from %s to %s", prevRequestIdVal, c.requestIdFromContext);
        }, prevStageContextValue -> {
            ContextHolder contextHolderOnSubJob = new ContextHolder();
            contextHolderOnSubJob.fillValuesFromContexts();
            Assertions.assertEquals(upstreamThreadContextHolder, contextHolderOnSubJob);
            Assertions.assertNotEquals(contextHolderOnSubJob, prevStageContextValue);
        });

        ContextHolder contextHolderAfterPropagation = new ContextHolder();
        contextHolderAfterPropagation.fillValuesFromContexts();

        Assertions.assertEquals(upstreamThreadContextHolder, contextHolderAfterPropagation);
    }

    private Uni<ContextHolder> getContextHolderUni(Consumer<ContextHolder> contextChangeFunc) {
        return Uni.createFrom().item(new ContextHolder())
                .onFailure()
                .recoverWithItem(() -> null)
                .onItem()
                .transform(contextHolder -> {
                    contextHolder.fillValuesFromContexts();
                    contextChangeFunc.accept(contextHolder);
                    return contextHolder;
                });
    }

    private Cancellable performAsyncThenSubscribeOnResultContext(ExecutorService subscriptionRunner,
                                                                 Consumer<ContextHolder> contextChangeFunc,
                                                                 Consumer<ContextHolder> assertOp) {
        return getContextHolderUni(contextChangeFunc).runSubscriptionOn(subscriptionRunner)
                .subscribe()
                .with(assertOp);
    }

    private ContextHolder getContextHolderOnUni(Consumer<ContextHolder> contextChangeFunc) {
        return getContextHolderUni(contextChangeFunc)
                .await()
                .indefinitely();
    }

    private static class ContextHolder {
        private static final Logger log = Logger.getLogger(ContextHolder.class);
        Map<String, Object> context;
        String requestIdFromMdc;
        String requestIdFromContext;
        String xVersionFromContext;

        void fillValuesFromContexts() {
            requestIdFromMdc = MDC.get("requestId");
            requestIdFromContext = RequestIdContext.get();
            xVersionFromContext = ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getXVersion();
            context = ContextManager.createContextSnapshot();
            log.infof("reqIdMdc=%s, reqIdCtx=%s, xVersion=%s, thread=%s", requestIdFromMdc, requestIdFromContext, xVersionFromContext, Thread.currentThread().getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContextHolder that = (ContextHolder) o;
            return requestIdFromMdc.equals(that.requestIdFromMdc) && requestIdFromContext.equals(that.requestIdFromContext);
        }

        @Override
        public String toString() {
            return "ContextHolder{" +
                    "requestIdFromMdc='" + requestIdFromMdc + '\'' +
                    ", requestIdFromContext='" + requestIdFromContext + '\'' +
                    ", xVersionFromContext='" + xVersionFromContext + '\'' +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(requestIdFromMdc, requestIdFromContext, xVersionFromContext);
        }
    }
}
