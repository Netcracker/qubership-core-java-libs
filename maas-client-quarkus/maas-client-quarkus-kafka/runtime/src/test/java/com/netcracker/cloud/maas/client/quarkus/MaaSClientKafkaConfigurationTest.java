package org.qubership.cloud.maas.client.quarkus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaaSClientKafkaConfigurationTest {

    @Mock
    private MaaSAPIClient maasAPIClient;

    @Mock
    private KafkaMaaSClient kafkaMaaSClient;

    private MaaSClientKafkaConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new MaaSClientKafkaConfiguration();
        when(maasAPIClient.getKafkaClient()).thenReturn(kafkaMaaSClient);
    }

    @Test
    void getKafkaMaaSClient_ShouldReturnClientFromMaaSAPIClient() {
        // Act
        KafkaMaaSClient result = configuration.getKafkaMaaSClient(maasAPIClient);

        // Assert
        assertNotNull(result, "Kafka client should not be null");
        assertEquals(kafkaMaaSClient, result, "Should return the client from MaaSAPIClient");
        verify(maasAPIClient, times(1)).getKafkaClient();
    }

    @Test
    void getKafkaMaaSClient_ShouldReturnSameInstanceForMultipleCalls() {
        // Act
        KafkaMaaSClient result1 = configuration.getKafkaMaaSClient(maasAPIClient);
        KafkaMaaSClient result2 = configuration.getKafkaMaaSClient(maasAPIClient);

        // Assert
        assertNotNull(result1, "First Kafka client should not be null");
        assertNotNull(result2, "Second Kafka client should not be null");
        assertSame(result1, result2, "Should return the same instance due to @Singleton annotation");
        verify(maasAPIClient, times(2)).getKafkaClient();
    }
}
