package com.netcracker.cloud.context.propagation.spring.webclient.interceptor;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.reactivestreams.Subscription;
import org.springframework.lang.Nullable;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.List;

public class CoreContextPropagator {
    private static final String CONTEXT_HOLDER_KEY = CoreContextPropagator.class.getName().concat(".CONTEXT_HOLDER");

    public static void installHook() {
        Hooks.onLastOperator(CoreContextPropagator.class.getName(), Operators.liftPublisher((p, sub) -> createContextSubscriber(sub)));
    }

    @Nullable
    static List<Object> getIfPresent(ContextView contextView) {
        return contextView.getOrDefault(CONTEXT_HOLDER_KEY, null);
    }

    private static <T> CoreSubscriber<T> createContextSubscriber(CoreSubscriber<T> originalSubscriber) {
        if (originalSubscriber.currentContext().hasKey(CONTEXT_HOLDER_KEY)) {
            return originalSubscriber;
        } else {
            return new CoreContextPropagatorSubscriber<>(originalSubscriber, ContextManager.getAll());
        }
    }

    private static class CoreContextPropagatorSubscriber<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<T> delegate;
        private final Context context;

        private CoreContextPropagatorSubscriber(CoreSubscriber<T> delegate,
                                            List<Object> contextObjects) {
            this.delegate = delegate;
            Context parentContext = this.delegate.currentContext();
            this.context = parentContext.put(CONTEXT_HOLDER_KEY, contextObjects);
        }

        @Override
        public Context currentContext() {
            return this.context;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            this.delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            this.delegate.onError(t);
        }

        @Override
        public void onComplete() {
            this.delegate.onComplete();
        }
    }
}
