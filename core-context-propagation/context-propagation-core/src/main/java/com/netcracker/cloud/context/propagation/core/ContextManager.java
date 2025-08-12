package com.netcracker.cloud.context.propagation.core;

import com.netcracker.cloud.context.propagation.core.contextdata.DeserializedIncomingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.netcracker.cloud.context.propagation.core.ContextProviderLoader.loadContextProviders;
import static com.netcracker.cloud.context.propagation.core.Scope.NOOP_SCOPE;

/**
 * ContextManager is the main class that allows operating on registered context. All manipulations on
 * context must be doing through ContextManager.<p>
 * <p>
 * In order to register you context in ContextManager you need to implement {@link Strategy},
 * {@link ContextProvider} and annotate your context provider by {@link RegisterProvider} annotation.
 */
public class ContextManager {

    @Deprecated
    public static final String LOOKUP_CONTEXT_PROVIDERS_PATH = "context_propagation.context_providers.path";
    public static final String CORE_CONTEXTPROPAGATION_PROVIDERS_LOOKUP = "core.contextpropagation.providers.lookup";

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    private static Map<String, ContextProvider<?>> registry = new HashMap<>();
    private static List<ContextProvider<?>> sortedContextProviders = new ArrayList<>();


    static {
        init();
    }

    public static void init() {
        String providersLookup = System.getProperty(CORE_CONTEXTPROPAGATION_PROVIDERS_LOOKUP, "true");
        if ("true".equals(providersLookup)) {
            register(loadContextProviders());
        }
    }

    private ContextManager() {
    }

    public static Collection<ContextProvider<?>> getContextProviders() {
        return sortedContextProviders;
    }

    /**
     * NotThreadSafe
     */
    public static Collection<ContextProvider<?>> register(List<ContextProvider<?>> providers) {
        for (ContextProvider<?> provider : providers) {
            ContextProvider<?> existedProvider = registry.get(provider.contextName());
            if (existedProvider != null) {
                if (isTheSame(provider, existedProvider) || isExistedLessOrder(provider, existedProvider)) {
                    continue;
                }
            }
            log.debug("Add context provider {} to context manager registry", provider);
            registry.put(provider.contextName(), provider);
        }
        sortByInitLevel(registry.values());
        return sortedContextProviders;
    }

    private static boolean isTheSame(ContextProvider<?> provider, ContextProvider<?> existedProvider) {
        return existedProvider.getClass().getName().equals(provider.getClass().getName());
    }

    private static boolean isExistedLessOrder(ContextProvider<?> provider, ContextProvider<?> existedProvider) {
        if (existedProvider.providerOrder() == provider.providerOrder()) {
            log.error("Context providers {}, {} must have different provider order values", existedProvider, provider);
            throw new RuntimeException(String.format("Found providers with identical name %s but " +
                    "they must have different provider order values. Context provider with lower value will be used", existedProvider.contextName()));
        }
        return existedProvider.providerOrder() < provider.providerOrder();
    }

    public static <V> void set(String contextName, V value) {
        Strategy<V> strategy = (Strategy<V>) getStrategy(contextName);
        strategy.set(value);
    }

    public static <V> V get(String contextName) {
        return (V) getStrategy(contextName).get();
    }

    public static <V> Optional<V> getSafe(String contextName) {
        return (Optional<V>) getStrategy(contextName).getSafe();
    }

    private static Strategy<?> getStrategy(String contextName) {
        ContextProvider<?> contextProvider = registry.get(contextName);
        if (contextProvider == null) {
            throw new RuntimeException("Context with name " + contextName + " is not registered and context provider can not be found");
        }
        return contextProvider.strategy();
    }

    public static void clear(String contextName) {
        getStrategy(contextName).clear();
    }

    public static void clearAll() {
        registry.keySet().forEach(ContextManager::clear);
    }

    public static List<Object> getAll() {
        return sortedContextProviders.stream()
                .map(contextProvider -> contextProvider.strategy().getSafe())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static <V> Scope newScope(String contextName, V value) {
        Strategy<V> strategy = (Strategy<V>) getStrategy(contextName);
        V previousValue = strategy.getSafe().orElse(null);
        strategy.set(value);
        if (previousValue == null) {
            return strategy::clear;
        } else if (previousValue == value) {
            return NOOP_SCOPE;
        }
        return () -> strategy.set(previousValue);
    }

    public static Map<String, Object> createContextSnapshot(@Nullable Set<String> contextNames) {
        if (contextNames == null || contextNames.isEmpty()) {
            contextNames = registry.keySet();
        }
        return contextNames.stream()
                .filter(contextName -> getStrategy(contextName).getSafe().isPresent())
                .collect(Collectors.toMap(contextName -> contextName, contextName -> getStrategy(contextName).get()));
    }

    public static Map<String, Object> createContextSnapshotWithoutContexts(@Nullable Set<String> excludedContexts) {
        if (excludedContexts == null) {
            excludedContexts = Collections.emptySet();
        }
        Set<String> finalExcludedContexts = excludedContexts;
        return registry.keySet().stream()
                .filter(contextName -> !finalExcludedContexts.contains(contextName))
                .filter(contextName -> getStrategy(contextName).getSafe().isPresent())
                .collect(Collectors.toMap(contextName -> contextName, contextName -> getStrategy(contextName).get()));
    }

    public static void activateContextSnapshot(Map<String, Object> context) {
        clearAll();
        for (Map.Entry<String, Object> stringObjectEntry : context.entrySet()) {
            Strategy<Object> strategy = (Strategy<Object>) getStrategy(stringObjectEntry.getKey());
            strategy.set(stringObjectEntry.getValue());
        }
    }

    public static <V> V executeWithContext(Map<String, Object> context, Supplier<V> supplier) {
        Map<String, Object> currentContext = createContextSnapshot();
        try {
            activateContextSnapshot(context);
            return supplier.get();
        } finally {
            activateContextSnapshot(currentContext);
        }
    }

    public static Map<String, Map<String, Object>> getSerializableContextData() {
        return getSerializableContextData(null);
    }

    public static Map<String, Map<String, Object>> getSerializableContextData(@Nullable Set<String> excludedContexts) {
        Map<String, Map<String, Object>> contextData = new HashMap<>();
        for (String contextName : registry.keySet()) {
            if (excludedContexts != null && excludedContexts.contains(contextName)) {
                continue;
            }
            Optional<?> contextObjectOpt = getStrategy(contextName).getSafe();
            if (contextObjectOpt.isPresent() && contextObjectOpt.get() instanceof SerializableDataContext) {
                Map<String, Object> data = ((SerializableDataContext) contextObjectOpt.get()).getSerializableContextData();
                if (data != null) {
                    contextData.put(contextName, data);
                }
            }
        }
        return contextData;
    }

    public static void activateWithSerializableContextData(Map<String, Map<String, Object>> contextData) {
        clearAll();
        for (Map.Entry<String, Map<String, Object>> ctxNameAndData : contextData.entrySet()) {
            ContextProvider contextProvider = registry.get(ctxNameAndData.getKey());
            if (contextProvider != null) {
                Object providedObj = contextProvider.provideFromSerializableData(new DeserializedIncomingContextData(ctxNameAndData.getValue()));
                contextProvider.strategy().set(providedObj);
            }
        }
    }

    public static Map<String, Object> createContextSnapshot() {
        return createContextSnapshot(null);
    }

    private static void sortByInitLevel(Collection<ContextProvider<?>> providers) {
        sortedContextProviders = new ArrayList<>(providers);
        sortedContextProviders.sort(Comparator.comparingInt(ContextProvider::initLevel));
    }

}
