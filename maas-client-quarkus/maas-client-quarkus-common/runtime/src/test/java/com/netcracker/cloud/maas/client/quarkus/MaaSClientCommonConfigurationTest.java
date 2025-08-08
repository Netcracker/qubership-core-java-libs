package com.netcracker.cloud.maas.client.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MaaSClientCommonConfigurationTest {

    @Inject
    Instance<MaaSAPIClient> clientInstance;

    @Test
    void getMaaSAPIClient_ShouldBeInjectableAndDefaultBean() {
        // When
        MaaSAPIClient client = clientInstance.get();

        // Then
        assertNotNull(client, "Client should be injectable and not null");
        assertInstanceOf(MaaSAPIClientImpl.class, client, "Client should be an instance of MaaSAPIClientImpl");
        // Verify it's the default bean by checking if it's the only instance available
        assertTrue(clientInstance.isResolvable(), "Client should be resolvable as a default bean");
        assertFalse(clientInstance.isAmbiguous(), "Client should not be ambiguous");
    }

    @Test
    void getMaaSAPIClient_ShouldMaintainSingletonScope() {
        // When
        MaaSAPIClient client1 = clientInstance.get();
        MaaSAPIClient client2 = clientInstance.get();
        MaaSAPIClient client3 = clientInstance.get();

        // Then
        assertSame(client1, client2, "First and second instances should be the same due to @Singleton annotation");
        assertSame(client2, client3, "Second and third instances should be the same due to @Singleton annotation");
        assertSame(client1, client3, "First and third instances should be the same due to @Singleton annotation");
    }
}
