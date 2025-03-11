package org.qubership.cloud.core.quarkus.dbaas.datasource.classifier;

import org.qubership.cloud.framework.contexts.tenant.TenantProvider;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DataSourceConfiguration;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TenantClassifierBuilderTest {

    @BeforeAll
    public static void initContext() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @Test
    void classifierBuilderMustBeThreadSafe() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        TenantClassifierBuilder b = new TenantClassifierBuilder(new HashMap<>());
        CountDownLatch countdown = new CountDownLatch(1);

        final class TestTask implements Runnable {
            String inputTenantId;
            DbaasDbClassifier builtClassifier;

            @SneakyThrows
            @Override
            public void run() {
                TenantContext.set(inputTenantId);
                countdown.await();
                builtClassifier = b.build();
            }
        }

        String[] tenants = new String[]{"a", "b", "c", "d", "e"};
        TestTask[] tasks = new TestTask[5];
        for (int i = 0; i < tenants.length; i++) {
            tasks[i] = new TestTask();
            tasks[i].inputTenantId = tenants[i];
        }

        ArrayList<Future<?>> futures = new ArrayList<>();
        Arrays.stream(tasks).forEach(t -> futures.add(executorService.submit(t)));

        countdown.countDown();

        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e.getMessage());
            }
        });

        for (int i = 0; i < tenants.length; i++) {
            Assertions.assertEquals(tasks[i].inputTenantId, tasks[i].builtClassifier.asMap().get("tenantId"));
        }
    }

    @Test
    void testCorrectBaseClassifierCreation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        DataSourceConfiguration dataSourceConfiguration = new DataSourceConfiguration();
        Method method = dataSourceConfiguration.getClass().getDeclaredMethod("getInitialClassifierMap");
        method.setAccessible(true);

        Field namespace = dataSourceConfiguration.getClass().getDeclaredField("namespace");
        namespace.setAccessible(true);
        namespace.set(dataSourceConfiguration, TEST_NAMESPACE);

        Field microserviceName = dataSourceConfiguration.getClass().getDeclaredField("microserviceName");
        microserviceName.setAccessible(true);
        microserviceName.set(dataSourceConfiguration, "test-microserviceName");

        Map<String, Object> classifeir = new HashMap<>();
        classifeir.put("microserviceName", "test-microserviceName");
        classifeir.put("namespace", TEST_NAMESPACE);
        assertEquals(classifeir, method.invoke(dataSourceConfiguration));
    }
}
