package com.netcracker.cloud.context.propagation.core;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.jetbrains.annotations.Nullable;

/**
 * Context provider provides information about context to contextManager.
 * Each context has own context provider, in other word context provider per context.<p>
 *
 * Notes: Context provider must have constructor without parameters (default constructor).
 *
 * @param <V> Context Object, the object contains fields and data which context should store.
 *            Context object can be wrap primitive types, third party or your own class.
 */
public interface ContextProvider<V> {
    /**
     * @return {@link Strategy} that will be used for storing context object.
     */
    Strategy<V> strategy();

    /**
     * Determined context level order. ContextManager sorts context providers by levels and
     * performs bulk operations (init, clear and so on) with contexts
     * with a lower level at first and then ascending levels.
     *<p>
     * if you don't care about the order of the context among other contexts then method can return 0 value.
     *<p>
     * Smaller will be done first
     * @return context level order
     */
    int initLevel();

    /**
     * Determined context init step. At what step the context will be initialized.
     *<p>
     * By default, the context is initialized after authentication.
     *<p>
     * @return context init step
     */
    default ContextInitializationStep getInitializationStep() {
        return ContextInitializationStep.POST_AUTHENTICATION;
    }

    /**
     * Determines which of several context providers with the same name should be used.
     * If there are several context providers with the same name
     * and their provider orders are equal then runtime exception will be.
     * <p>
     * We recommend to use 0 if you write your own and don't override existed context. <p>
     * If you override existed context then value should be multiple of 100. For example: 0, -100, -200 <p>
     * Context provider with smaller value wins.
     * @return value which determines which context with the same name will be used.
     */
    int providerOrder();

    /**
     * The name of context. ContextName is unique key of context. By this name you can get or set context object in context.
     * Can't be registered more than one context in contextManager with the same name. <p>
     *
     * Additionally, we strongly recommend to make method realization as final because class that overrides existed
     * context must have the same name.
     *
     * @return Context name
     */
    String contextName();

    /**
     * The method creates contextObject. Context object may be initialized based on data from {@link IncomingContextData}
     * For example if context is serialized and propagated from microservice to microservice
     * then this method should describe how context object can be deserialized.<p>
     *
     * If incomingContextData is not null and there are not data relevant to this context, method should return null.
     *
     * @param incomingContextData contains data with which context object can be deserialized.
     * @return Context object or null
     */
    V provide(@Nullable IncomingContextData incomingContextData);

    /**
     * The method creates contextObject from context data which context had provided by {@link SerializableDataContext#getSerializableContextData()}.<p>
     * Mostly initialization process is the same as {@link  #provide(IncomingContextData)}
     * but there are some cases when a process of building context between rest and serialized data is different. <p>
     *
     * @param incomingContextData contains serialized context data from which context can be built.
     * @return restore context object
     */
    default V provideFromSerializableData(IncomingContextData incomingContextData) {
        return provide(incomingContextData);
    }
}
