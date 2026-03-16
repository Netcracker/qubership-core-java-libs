package com.netcracker.cloud.context.propagation.core;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.netcracker.cloud.context.propagation.core.ContextManager.LOOKUP_CONTEXT_PROVIDERS_PATH;


class ContextProviderLoader {

    static final String JANDEX_INDEX = "META-INF/jandex.idx";

    private static final String DISABLE_REFLECTION_DISCOVERY_PROPERTY = "core.contextpropagation.providers.reflection-discovery.disabled";
    private static final Logger log = LoggerFactory.getLogger(ContextProviderLoader.class);

    private ContextProviderLoader() {
    }

    static List<ContextProvider<?>> loadContextProviders() {
        boolean useReflection = !"true".equals(System.getProperty(DISABLE_REFLECTION_DISCOVERY_PROPERTY, "false"));

        Set<Class<?>> jandexProviderClasses = loadProvidersFromJandex(JANDEX_INDEX);

        if (useReflection) {
            Set<Class<?>> reflectionProviderClasses = loadProvidersByReflection();
            for (Class<?> providerFromReflection : reflectionProviderClasses) {
                if (!jandexProviderClasses.contains(providerFromReflection)) {
                    log.warn("Context provider {} from {} was discovered using the classpath scanning approach. " +
                                    "This method is deprecated and will be removed in the next major release. " +
                                    "Please refer to the context-propagation guide and migrate to the Jandex discovery method.",
                            providerFromReflection, providerFromReflection.getProtectionDomain().getCodeSource().getLocation());
                }
            }
            jandexProviderClasses.addAll(reflectionProviderClasses);

        }
        return initProviders(jandexProviderClasses);
    }

    private static Set<Class<?>> loadProvidersByReflection() {
        Reflections reflections = new Reflections(getPrefixPath(), new TypeAnnotationsScanner());
        Set<Class<?>> providerClasses = reflections.getTypesAnnotatedWith(RegisterProvider.class, true);
        return providerClasses;
    }

    static Set<Class<?>> loadProvidersFromJandex(String indexPath) {
        Set<Class<?>> providerClasses = new HashSet<>();
        try {
            Enumeration<URL> indexResources = Thread.currentThread().getContextClassLoader().getResources(indexPath);
            while (indexResources.hasMoreElements()) {
                try (InputStream indexStream = indexResources.nextElement().openStream()) {
                    IndexReader indexReader = new IndexReader(indexStream);
                    Index index = indexReader.read();
                    Collection<AnnotationInstance> annotationInstances = index.getAnnotations(RegisterProvider.class);
                    for (AnnotationInstance annotationInstance : annotationInstances) {
                        AnnotationTarget annotationTarget = annotationInstance.target();
                        if (annotationTarget.kind() == AnnotationTarget.Kind.CLASS) {
                            Class<?> annotatedClass = Thread.currentThread().getContextClassLoader().loadClass(annotationTarget.toString());
                            providerClasses.add(annotatedClass);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot load jandex index", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load annotated provider class", e);
        }
        log.debug("Found providers in jandex: {}", providerClasses);
        return providerClasses;
    }

    private static String[] getPrefixPath() {
        String prefixPath = System.getProperty(LOOKUP_CONTEXT_PROVIDERS_PATH, "com.netcracker.cloud,com");
        log.debug("Prefix path: {} will be used to find all context providers under this path", prefixPath);
        return prefixPath.split(",");
    }

    static List<ContextProvider<?>> initProviders(Set<Class<?>> providerClasses) {
        List<ContextProvider<?>> contextProviders = new ArrayList<>();
        for (Class<?> aClass : providerClasses) {
            try {
                ContextProvider contextProvider = (ContextProvider) aClass.newInstance();
                contextProviders.add(contextProvider);
            } catch (Exception e) {
                log.error("Error during registration {} provider: ", aClass, e);
                throw new RuntimeException(e);
            }
        }
        return contextProviders;
    }
}
