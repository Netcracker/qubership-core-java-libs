package com.netcracker.cloud.maas.client.context.kafka;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MaaSKafkaAdminWrapperTest {

    @Test
    void registerAndUnregisterMetricForSubscription_delegateToUnderlyingAdmin() throws Exception {
        Map<String, Object> configs = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        KafkaMaaSClient kafkaMaaSClient = mock(KafkaMaaSClient.class);
        Admin underlyingAdmin = mock(Admin.class);
        KafkaMetric metric = mock(KafkaMetric.class);

        try (MaaSKafkaAdminWrapper wrapper = MaaSKafkaAdminWrapper.builder(configs, kafkaMaaSClient).build()) {
            replaceDelegate(wrapper, underlyingAdmin);

            assertDoesNotThrow(() -> wrapper.registerMetricForSubscription(metric));
            assertDoesNotThrow(() -> wrapper.unregisterMetricFromSubscription(metric));

            verify(underlyingAdmin).registerMetricForSubscription(metric);
            verify(underlyingAdmin).unregisterMetricFromSubscription(metric);
        }
    }

    @Test
    void forwardingAdmin_stillThrowsUnsupportedOperationException_forMetricsMethods() {
        Map<String, Object> configs = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        try (ForwardingAdmin forwardingAdmin = new ForwardingAdmin(configs)) {
            KafkaMetric metric = mock(KafkaMetric.class);
            assertThrows(UnsupportedOperationException.class,
                    () -> forwardingAdmin.registerMetricForSubscription(metric));
            assertThrows(UnsupportedOperationException.class,
                    () -> forwardingAdmin.unregisterMetricFromSubscription(metric));
        }
    }

    private static void replaceDelegate(ForwardingAdmin admin, Admin delegate) throws Exception {
        Field delegateField = ForwardingAdmin.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        Admin previous = (Admin) delegateField.get(admin);
        delegateField.set(admin, delegate);
        if (previous != null) {
            previous.close();
        }
    }
}
