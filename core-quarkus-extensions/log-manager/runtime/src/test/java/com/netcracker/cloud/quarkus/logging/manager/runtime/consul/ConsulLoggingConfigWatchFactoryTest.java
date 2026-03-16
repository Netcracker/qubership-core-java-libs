package com.netcracker.cloud.quarkus.logging.manager.runtime.consul;

import org.junit.jupiter.api.Assertions;
import com.netcracker.cloud.quarkus.consul.client.ConsulClient;
import com.netcracker.cloud.quarkus.consul.client.ConsulSourceConfig;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import com.netcracker.cloud.quarkus.consul.client.http.Response;
import com.netcracker.cloud.quarkus.consul.client.http.QueryParams;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulLoggingConfigWatchFactoryTest {

    private static final String[] prefixes = new String[] { "test/null", "test/application", "test/app-name" };
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
                return 600;
            }
        };
        this.consulLoggingSourceConfig = new ConsulLoggingSourceConfig() {

            @Override
            public boolean loggingEnabled() {
                return true;
            }

            @Override
            public Integer consulRetryTime() {
                return 20000;
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

        Response<List<GetValue>> listResponse = new Response<>(Collections.singletonList(getValue), 1L, true, 1L);
        when(consulClient.getKVValues(eq(""), anyString())).thenReturn(listResponse);

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

        Response<List<GetValue>> listResponse1 = new Response<>(Collections.singletonList(getValueWithNullValue), 1L, true, 1L);
        Response<List<GetValue>> listResponse2 = new Response<>(Collections.singletonList(getValue), 2L, true, 1L);
        Response<List<GetValue>> nullResponse = new Response<>(null, 3L, true, 1L);
        when(consulClient.getKVValues(eq(""), anyString())).thenReturn(listResponse1);

        when(consulClient.getKVValues(eq(""), anyString(), any())).thenAnswer(invocation -> {
            QueryParams param = invocation.getArgument(2);
            if (param.getIndex() == 1) {
                return listResponse2;
            }
            return nullResponse;
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
        Response<List<GetValue>> listResponse = new Response<>(Collections.singletonList(getValue), 1L, true, 1L);

        when(consulClient.getKVValues(eq(""), anyString())).thenAnswer(i -> listResponse);

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
        factory.runConsulLoggingWatcher(consulSourceConfig, consulLoggingSourceConfig);
        factoryInitiatedFuture.complete(null);

        Map<String, String> properties = consulPropertiesReceivedFuture.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
    }
}
