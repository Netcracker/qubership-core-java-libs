package org.qubership.maas.declarative.kafka.client.impl.topic.provider.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import org.qubership.maas.declarative.kafka.client.impl.topic.LocalDevMaasDirectKafkaTopicServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalDevMaasDirectKafkaTopicServiceProviderImplTest {

    @Mock
    private MaasKafkaLocalDevConfigProviderService configProviderService;

    private LocalDevMaasDirectKafkaTopicServiceProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new LocalDevMaasDirectKafkaTopicServiceProviderImpl(configProviderService);
    }

    @Test
    void provide_ShouldReturnLocalDevMaasDirectKafkaTopicService() {
        // When
        MaasKafkaTopicService service = provider.provide();

        // Then
        assertNotNull(service);
        assertEquals(LocalDevMaasDirectKafkaTopicServiceImpl.class, service.getClass());
    }

    @Test
    void order_ShouldReturnOneLessThanDefault() {
        // When
        int order = provider.order();

        // Then
        assertEquals(Integer.MAX_VALUE - 1, order);
    }
}
