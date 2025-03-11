package org.qubership.cloud.consul.config.source.runtime;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.qubership.cloud.consul.config.source.runtime.ConsulConfigSourceFactory.BASE_CONFIG_SOURCE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulConfigSourceFactoryTest {

    private static final String[] prefixes = new String[]{"test/null", "test/application", "test/app-name"};
    ConsulSourceConfig consulDefaultSourceConfig;

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
    }

    @Test
    void getConfigSources() {
        ConsulClient consulClient = mock(ConsulClient.class);
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn("token");
        Response<List<GetValue>> listResponse = new Response<>(null, 1L, true, 1L);
        when(consulClient.getKVValues(eq(prefixes[0]), anyString())).thenReturn(listResponse);

        GetValue getValue = new GetValue();
        getValue.setKey(prefixes[1] + "/test-prop");
        getValue.setValue(Base64.getEncoder().encodeToString((prefixes[1] + "-value").getBytes()));
        listResponse = new Response<>(Collections.singletonList(getValue), 1L, true, 1L);
        when(consulClient.getKVValues(eq(prefixes[1]), anyString())).thenReturn(listResponse);
        when(consulClient.getKVValues(eq(prefixes[1]), anyString(), any())).thenReturn(listResponse);

        getValue = new GetValue();
        getValue.setKey(prefixes[2] + "/test-prop");
        getValue.setValue(Base64.getEncoder().encodeToString((prefixes[2] + "-value").getBytes()));
        listResponse = new Response<>(Collections.singletonList(getValue), 1L, true, 1L);
        when(consulClient.getKVValues(eq(prefixes[2]), anyString())).thenReturn(listResponse);
        when(consulClient.getKVValues(eq(prefixes[2]), anyString(), any())).thenReturn(listResponse);

        ConsulConfigSourceFactory factory = new ConsulConfigSourceFactory(consulClient, tokenStorage);
        Spliterator<ConfigSource> configSourcesSpliterator = factory.getConfigSources(null, consulDefaultSourceConfig).spliterator();
        List<ConfigSource> configSources = StreamSupport.stream(configSourcesSpliterator, false).collect(Collectors.toList());

        assertEquals(3, configSources.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(BASE_CONFIG_SOURCE_NAME + "-" + prefixes[prefixes.length - i - 1], configSources.get(i).getName());
            assertEquals(prefixes[prefixes.length - i - 1] + "-value", configSources.get(i).getValue("test-prop"));
        }
        assertTrue(configSources.get(2).getProperties().isEmpty());
    }
}