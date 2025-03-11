package org.qubership.cloud.quarkus.logging.manager.runtime.consul;

import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void initConsulLoggingWatch_fireLogUpdated() throws InterruptedException {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);
        GetValue getValue = new GetValue();
        getValue.setKey("logging/test-ns/test-app/logging.level.com.test");
        getValue.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));

        Response<List<GetValue>> listResponse = new Response<>(Collections.singletonList(getValue), 1L, true, 1L);
        when(consulClient.getKVValues(eq("logging/test-ns/test-app"), anyString())).thenReturn(listResponse);
        when(consulClient.getKVValues(eq("logging/test-ns/test-app"), anyString(), any())).thenReturn(listResponse);

        ConsulLoggingConfigWatchFactory factory = spy(new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub()));
        doNothing().when(factory).firePropertiesUpdated(anyMap(), anyString());
        doNothing().when(factory).fireLogUpdated(anyMap(), anyString());

        factory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);
        sleep(5000);
        verify(factory).fireLogUpdated(anyMap(), anyString());
    }

    @Test
    void initConsulLoggingWatch_fireLogUpdated_whenConsulEmpty() throws InterruptedException {
        ConsulClient consulClient = mock(ConsulClient.class);
        ConsulSourceConfig consulSourceConfig = mock(ConsulSourceConfig.class);
        GetValue getValueWithNullValue = new GetValue();
        getValueWithNullValue.setKey("logging/test-ns/test-app");
        getValueWithNullValue.setValue(null);
        
        GetValue getValue = new GetValue();
        getValue.setKey("logging/test-ns/test-app/logging.level.com.test");
        getValue.setValue(Base64.getEncoder().encodeToString(("DEBUG").getBytes()));
        
        Response<List<GetValue>> listResponse1 = new Response<>(Collections.singletonList(getValueWithNullValue), 1L, true, 1L);
        Response<List<GetValue>> listResponse2 = new Response<>(Collections.singletonList(getValueWithNullValue), 2L, true, 1L);
        Response<List<GetValue>> listResponse3 = new Response<>(Collections.singletonList(getValue), 3L, true, 1L);
        Response<List<GetValue>> nullResponse = new Response<>(null, 3L, true, 1L);
        when(consulClient.getKVValues(eq("logging/test-ns/test-app"), anyString())).thenReturn(listResponse1);

        when(consulClient.getKVValues(eq("logging/test-ns/test-app"), anyString(), any())).thenAnswer(invocation -> {
            QueryParams param = invocation.getArgument(2);
            if (param.getIndex() == 1L) {
                return listResponse2;
            } else if (param.getIndex() == 2L) {
                return listResponse3;
            }
            return nullResponse;
        });

        ConsulLoggingConfigWatchFactory factory = spy(
                new ConsulLoggingConfigWatchFactory(consulClient, new TokenStorageStub()));
        doNothing().when(factory).firePropertiesUpdated(anyMap(), anyString());
        doNothing().when(factory).fireLogUpdated(anyMap(), anyString());

        factory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);
        sleep(5000);
        verify(factory, times(1)).fireLogUpdated(eq(Collections.emptyMap()), anyString());
        verify(factory, times(1)).firePropertiesUpdated(eq(Collections.emptyMap()), anyString());
        verify(factory, times(1)).firePropertiesUpdated(eq(Collections.singletonMap("logging.level.com.test", "DEBUG")), anyString());
    }
}