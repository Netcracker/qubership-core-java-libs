package org.qubership.cloud.testharness;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
public class MaaSCocoonExtension implements TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback {
    final Map<Class<? extends Annotation>, WebsockServer> injections = new HashMap<>();

    @Data
    @AllArgsConstructor
    class Capability {
        private final Class<? extends Annotation> annotation;
        private final Class<? extends WebsockServer> requiredType;
        private final Supplier<WebsockServer> constructor;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) {
        List<Capability> capabilities = new ArrayList<>();
        capabilities.add(new Capability(TenantManagerMockInject.class, TenantManagerMockServer.class, TenantManagerMockServer::new));

        Arrays.stream(testInstance.getClass().getDeclaredFields()).forEach(field -> {
            capabilities.stream().forEach(capability -> {
                if (field.isAnnotationPresent(capability.getAnnotation())) {
                    if(field.getType().equals(capability.getRequiredType())) {
                        try {
                            WebsockServer server = injections.computeIfAbsent(capability.getAnnotation(), k -> capability.getConstructor().get());
                            log.debug("Inject mock server instance {} to field: {}", server, field);
                            field.setAccessible(true);
                            field.set(testInstance, server);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Can't set value for control plane mock field", e);
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported type for injection to field: " + field.getDeclaringClass().getTypeName() + "#" + field.getName()
                                + "\n\ttype required: "  + capability.getRequiredType()
                                + "\n\tactual type: " + field.getType());
                    }
                }
            });
        });
    }


    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        log.info("Start mock server instances");
        injections.values().forEach(WebsockServer::start);
    }


    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        log.info("Stop mock server instances");
        injections.values().forEach(WebsockServer::stop);
    }
}
