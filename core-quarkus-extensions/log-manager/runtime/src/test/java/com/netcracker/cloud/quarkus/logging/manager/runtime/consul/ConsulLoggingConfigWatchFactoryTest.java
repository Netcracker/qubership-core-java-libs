package com.netcracker.cloud.quarkus.logging.manager.runtime.consul;

import com.netcracker.cloud.quarkus.consul.client.ConsulClient;
import com.netcracker.cloud.quarkus.consul.client.ConsulSourceConfig;
import com.netcracker.cloud.quarkus.consul.client.http.QueryParams;
import com.netcracker.cloud.quarkus.consul.client.http.Response;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulLoggingConfigWatchFactoryTest {

    private static final String[] prefixes = new String[]{"test/null", "test/application", "test/app-name"};
    ConsulSourceConfig consulDefaultSourceConfig;
    ConsulLoggingSourceConfig consulLoggingSourceConfig;

    @BeforeEach
    void setUp() {
        this.consulDefaultSourceConfig = new ConsulSourceConfig() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public AgentConfig agent() {
                return () -> Optional.of("http://test:8500");
            }

            @Override
            public Optional<List<String>> propertiesRoot() {
                return Optional.of(List.of(prefixes));
            }

            @Override
            public Integer waitTime() {
                return 5000;
            }
        };
        this.consulLoggingSourceConfig = new ConsulLoggingSourceConfig() {

            @Override
            public boolean loggingEnabled() {
                return true;
            }

            @Override
            public Integer consulRetryTime() {
                return 200;
            }

            @Override
            public int consulOnSuccessDelayTime() {
                return 100;
            }
        };
    }

    @Test
    void initConsulLoggingWatch_fireLogUpdated() throws Exception {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);
        GetValue getValue = new GetValue();
        getValue.setKey("logging/test-ns/test-ms/logging/level/com.test");
        getValue.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));

        Response<List<GetValue>> listResponse = new Response<>(List.of(getValue), 1L, true, 1L);

        when(consulClient.getKVValuesAsync(any(), any(), any())).thenAnswer(invocation -> {
            String root = invocation.getArgument(0);
            Response<List<GetValue>> response;
            if ("logging/test-ns/test-ms/".equals(root)) {
                response = listResponse;
            } else {
                response = null;
            }
            return CompletableFuture.completedFuture(response);
        });

        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        ConsulLoggingConfigWatchFactory factory = new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub(), "test-ns", "test-ms") {
            @Override
            protected void firePropertiesUpdated(Map<String, String> properties) {
                future.complete(properties);
            }
        };
        factory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);
        Map<String, String> properties = future.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("DEBUG", properties.get("quarkus.log.category.\"com.test\".level"));
    }

    @Test
    void initConsulLoggingWatch_fireLogUpdated_whenConsulEmpty() throws Exception {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);
        GetValue getValueWithNullValue = new GetValue();
        getValueWithNullValue.setKey("logging/test-ns/test-ms");
        getValueWithNullValue.setValue(null);

        GetValue getValue = new GetValue();
        getValue.setKey("logging/test-ns/test-ms/logging/level/com.test");
        getValue.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));

        Response<List<GetValue>> listResponse1 = new Response<>(List.of(getValueWithNullValue), 1L, true, 1L);
        Response<List<GetValue>> listResponse2 = new Response<>(List.of(getValue), 2L, true, 1L);
        Response<List<GetValue>> nullResponse = new Response<>(null, 3L, true, 1L);

        when(consulClient.getKVValuesAsync(any(), any(), any())).thenAnswer(invocation -> {
            String root = invocation.getArgument(0);
            QueryParams param = invocation.getArgument(2);
            Response<List<GetValue>> response;
            if ("logging/test-ns/test-ms/".equals(root)) {
                if (param.getIndex() == 0) {
                    response = listResponse1;
                } else if (param.getIndex() == 1) {
                    response = listResponse2;
                } else {
                    response = nullResponse;
                }
            } else {
                response = null;
            }
            return CompletableFuture.completedFuture(response);
        });

        ArrayBlockingQueue<Map<String, String>> future = new ArrayBlockingQueue<>(3);
        ConsulLoggingConfigWatchFactory factory = new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub(), "test-ns", "test-ms") {
            @Override
            protected void firePropertiesUpdated(Map<String, String> properties) {
                future.add(properties);
            }
        };

        factory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);
        Map<String, String> properties = future.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertTrue(properties.isEmpty());

        properties = future.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("DEBUG", properties.get("quarkus.log.category.\"com.test\".level"));
    }

    @Test
    void initConsulLoggingWatchInBackground() throws Exception {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);
        GetValue getValue = new GetValue();
        getValue.setKey("logging/test-ns/test-ms/logging/level/com.test");
        getValue.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));

        CompletableFuture<Void> factoryInitiatedFuture = new CompletableFuture<>();
        CompletableFuture<Map<String, String>> consulPropertiesReceivedFuture = new CompletableFuture<>();
        Response<List<GetValue>> listResponse = new Response<>(List.of(getValue), 1L, true, 1L);

        when(consulClient.getKVValuesAsync(any(), any(), any())).thenAnswer(invocation -> {
            String root = invocation.getArgument(0);
            Response<List<GetValue>> response;
            if ("logging/test-ns/test-ms/".equals(root)) {
                response = listResponse;
            } else {
                response = null;
            }
            return CompletableFuture.completedFuture(response);
        });

        ConsulLoggingConfigWatchFactory factory = new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub(), "test-ns", "test-ms") {
            @Override
            protected void firePropertiesUpdated(Map<String, String> properties) {
                try {
                    factoryInitiatedFuture.get(5, TimeUnit.SECONDS);
                    consulPropertiesReceivedFuture.complete(properties);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Integer waitTime = consulSourceConfig.waitTime();
        Integer consulRetryTime = consulLoggingSourceConfig.consulRetryTime();
        int onSuccessDelayTime = consulLoggingSourceConfig.consulOnSuccessDelayTime();
        factory.watchConsulLoggingRoot("logging/test-ns/test-ms/", waitTime, consulRetryTime, onSuccessDelayTime, 0);
        factoryInitiatedFuture.complete(null);

        Map<String, String> properties = consulPropertiesReceivedFuture.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
    }

    @Test
    void initConsulLoggingWatch_fireLogUpdated_whenConsulHas3Roots() throws Exception {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);

        GetValue getValue1 = new GetValue();
        getValue1.setKey("config/test-ns/application/quarkus.log.category.\"com.test\".level");
        getValue1.setValue(Base64.getEncoder().encodeToString(("INFO").getBytes()));

        GetValue getValue2 = new GetValue();
        getValue2.setKey("config/test-ns/test-ms/quarkus.log.category.\"com.test\".level");
        getValue2.setValue(Base64.getEncoder().encodeToString(("WARN").getBytes()));

        GetValue getValue3 = new GetValue();
        getValue3.setKey("logging/test-ns/test-ms/logging/level/com.test");
        getValue3.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));

        BlockingDeque<Response<List<GetValue>>> responseQueue1 = new LinkedBlockingDeque<>(1);
        BlockingDeque<Response<List<GetValue>>> responseQueue2 = new LinkedBlockingDeque<>(1);
        BlockingDeque<Response<List<GetValue>>> responseQueue3 = new LinkedBlockingDeque<>(1);

        when(consulClient.getKVValuesAsync(eq("config/test-ns/application/"), any(), any())).thenAnswer(i -> {
            QueryParams param = i.getArgument(2);
            Response<List<GetValue>> response;
            if (param.getIndex() == 0) {
                response = new Response<>(List.of(getValue1), 1L, true, 1L);
            } else {
                response = responseQueue1.take();
            }
            return CompletableFuture.completedFuture(response);
        });
        when(consulClient.getKVValuesAsync(eq("config/test-ns/test-ms/"), any(), any())).thenAnswer(i -> {
            QueryParams param = i.getArgument(2);
            Response<List<GetValue>> response;
            if (param.getIndex() == 0) {
                response = new Response<>(List.of(getValue2), 1L, true, 1L);
            } else {
                response = responseQueue2.take();
            }
            return CompletableFuture.completedFuture(response);
        });
        when(consulClient.getKVValuesAsync(eq("logging/test-ns/test-ms/"), any(), any())).thenAnswer(i -> {
            QueryParams param = i.getArgument(2);
            Response<List<GetValue>> response;
            if (param.getIndex() == 0) {
                response = new Response<>(List.of(getValue3), 1L, true, 1L);
            } else {
                response = responseQueue3.take();
            }
            return CompletableFuture.completedFuture(response);
        });

        ArrayBlockingQueue<Map<String, String>> future = new ArrayBlockingQueue<>(3);
        ConsulLoggingConfigWatchFactory factory = new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub(), "test-ns", "test-ms") {
            @Override
            protected void firePropertiesUpdated(Map<String, String> properties) {
                future.add(properties);
            }
        };

        factory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);

        int waitTimeSeconds = 1;

        Map<String, String> properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);

        properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);

        properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("DEBUG", properties.get("quarkus.log.category.\"com.test\".level"));

        getValue3.setValue(Base64.getEncoder().encodeToString(("ERROR").getBytes()));
        responseQueue3.add(new Response<>(List.of(getValue3), 2L, true, 1L));
        properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("ERROR", properties.get("quarkus.log.category.\"com.test\".level"));

        responseQueue3.add(new Response<>(List.of(), 3L, true, 1L));
        properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("WARN", properties.get("quarkus.log.category.\"com.test\".level"));

        responseQueue2.add(new Response<>(List.of(), 3L, true, 1L));
        properties = future.poll(waitTimeSeconds, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals("INFO", properties.get("quarkus.log.category.\"com.test\".level"));
    }
}
