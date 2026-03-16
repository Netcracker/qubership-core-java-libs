package com.netcracker.maas.declarative.kafka.client.impl.topic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaasKafkaAggregationTopicServiceTest {

    @Mock
    private MaasKafkaTopicServiceProvider provider1;
    @Mock
    private MaasKafkaTopicServiceProvider provider2;
    @Mock
    private MaasKafkaTopicService topicService1;
    @Mock
    private MaasKafkaTopicService topicService2;
    @Mock
    private MaasKafkaCommonClientDefinition clientDefinition;
    @Mock
    private TopicAddress topicAddress;

    private MaasKafkaAggregationTopicService service;
    private static final String TENANT_ID = "test-tenant";

    @Nested
    class ProviderOrderingTests {
        @Test
        void shouldCallProvidersInOrderOfPriority() {
            when(provider2.provide()).thenReturn(topicService2);

            when(provider1.order()).thenReturn(2);
            when(provider2.order()).thenReturn(1);

            service = new MaasKafkaAggregationTopicService(Arrays.asList(provider1, provider2));
            service.getTopicAddressByDefinition(clientDefinition);

            verify(provider2).provide();
            verify(provider1, never()).provide();
        }

        @Test
        void shouldUseFirstProviderWhenOrdersAreEqual() {
            when(provider1.provide()).thenReturn(topicService1);

            when(provider1.order()).thenReturn(1);
            when(provider2.order()).thenReturn(1);
            when(topicService1.getTopicAddressByDefinition(clientDefinition)).thenReturn(topicAddress);

            service = new MaasKafkaAggregationTopicService(Arrays.asList(provider1, provider2));

            TopicAddress result = service.getTopicAddressByDefinition(clientDefinition);
            assertEquals(topicAddress, result);
            verify(provider1).provide();
            verify(provider2, never()).provide();
        }
    }

    @Nested
    class ErrorHandlingTests {
        @Test
        void shouldTryNextProviderWhenCurrentThrowsException() {
            when(provider1.provide()).thenThrow(new RuntimeException("Test exception"));
            when(provider2.provide()).thenReturn(topicService2);
            when(topicService2.getTopicAddressByDefinition(clientDefinition)).thenReturn(topicAddress);

            service = new MaasKafkaAggregationTopicService(Arrays.asList(provider1, provider2));

            TopicAddress result = service.getTopicAddressByDefinition(clientDefinition);
            assertEquals(topicAddress, result);
            verify(provider1).provide();
            verify(provider2).provide();
            verify(topicService2).getTopicAddressByDefinition(clientDefinition);
        }

        @Test
        void shouldThrowExceptionWhenNoValidProvider() {
            when(provider1.provide()).thenReturn(null);
            service = new MaasKafkaAggregationTopicService(Collections.singletonList(provider1));

            assertThrows(MaasKafkaException.class, () -> service.getTopicAddressByDefinition(clientDefinition));
        }

        @Test
        void shouldThrowExceptionWhenProvidersListIsEmpty() {
            service = new MaasKafkaAggregationTopicService(Collections.emptyList());
            assertThrows(MaasKafkaException.class, () -> service.getTopicAddressByDefinition(clientDefinition));
        }
    }

    @Nested
    class TopicAddressOperationsTests {
        @BeforeEach
        void setUp() {
            when(provider1.provide()).thenReturn(topicService1);
            service = new MaasKafkaAggregationTopicService(Collections.singletonList(provider1));
        }

        @Test
        void shouldGetTopicAddressByDefinition() {
            when(topicService1.getTopicAddressByDefinition(clientDefinition)).thenReturn(topicAddress);

            TopicAddress result = service.getTopicAddressByDefinition(clientDefinition);
            assertEquals(topicAddress, result);
            verify(topicService1).getTopicAddressByDefinition(clientDefinition);
        }

        @Test
        void shouldGetTopicAddressByDefinitionAndTenantId() {
            when(topicService1.getTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID))
                    .thenReturn(topicAddress);

            TopicAddress result = service.getTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
            assertEquals(topicAddress, result);
            verify(topicService1).getTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        }

        @Test
        void shouldGetOrCreateTopicAddressByDefinition() {
            when(topicService1.getOrCreateTopicAddressByDefinition(clientDefinition)).thenReturn(topicAddress);

            TopicAddress result = service.getOrCreateTopicAddressByDefinition(clientDefinition);
            assertEquals(topicAddress, result);
            verify(topicService1).getOrCreateTopicAddressByDefinition(clientDefinition);
        }

        @Test
        void shouldGetOrCreateTopicAddressByDefinitionAndTenantId() {
            when(topicService1.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID))
                    .thenReturn(topicAddress);

            TopicAddress result = service.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
            assertEquals(topicAddress, result);
            verify(topicService1).getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        }
    }

    @Nested
    class TopicNameOperationsTests {
        @BeforeEach
        void setUp() {
            when(provider1.provide()).thenReturn(topicService1);
            service = new MaasKafkaAggregationTopicService(Collections.singletonList(provider1));
        }

        @Test
        void shouldGetFullyQualifiedTopicAddressName() {
            String expectedName = "test-topic";
            when(topicService1.getFullyQualifiedTopicAddressName(clientDefinition)).thenReturn(expectedName);

            String result = service.getFullyQualifiedTopicAddressName(clientDefinition);
            assertEquals(expectedName, result);
            verify(topicService1).getFullyQualifiedTopicAddressName(clientDefinition);
        }

        @Test
        void shouldGetFullyQualifiedTopicAddressNameByTenantId() {
            String expectedName = "test-topic";
            when(topicService1.getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, TENANT_ID))
                    .thenReturn(expectedName);

            String result = service.getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, TENANT_ID);
            assertEquals(expectedName, result);
            verify(topicService1).getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, TENANT_ID);
        }
    }
}
