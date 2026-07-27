package com.netcracker.cloud.maas.client.context.kafka;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MaaSKafkaAdminWrapperTest {

    private final MaaSKafkaAdminWrapper.DelegateFieldGetter originalDelegateFieldGetter =
            MaaSKafkaAdminWrapper.delegateFieldGetter;

    @AfterEach
    void restoreDelegateFieldGetter() {
        MaaSKafkaAdminWrapper.delegateFieldGetter = originalDelegateFieldGetter;
    }

    @Test
    void registerAndUnregisterMetricForSubscription_delegateToUnderlyingAdmin() {
        Map<String, Object> configs = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        KafkaMaaSClient kafkaMaaSClient = mock(KafkaMaaSClient.class);
        Admin underlyingAdmin = mock(Admin.class);
        KafkaMetric metric = mock(KafkaMetric.class);

        MaaSKafkaAdminWrapper.delegateFieldGetter = target -> underlyingAdmin;

        try (MaaSKafkaAdminWrapper wrapper = MaaSKafkaAdminWrapper.builder(configs, kafkaMaaSClient).build()) {
            wrapper.registerMetricForSubscription(metric);
            wrapper.unregisterMetricFromSubscription(metric);

            verify(underlyingAdmin).registerMetricForSubscription(metric);
            verify(underlyingAdmin).unregisterMetricFromSubscription(metric);
        }
    }

    @Test
    void registerMetricForSubscription_wrapsIllegalAccessException() {
        Map<String, Object> configs = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        KafkaMaaSClient kafkaMaaSClient = mock(KafkaMaaSClient.class);
        KafkaMetric metric = mock(KafkaMetric.class);

        MaaSKafkaAdminWrapper.delegateFieldGetter = target -> {
            throw new IllegalAccessException("forced for test");
        };

        try (MaaSKafkaAdminWrapper wrapper = MaaSKafkaAdminWrapper.builder(configs, kafkaMaaSClient).build()) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> wrapper.registerMetricForSubscription(metric));
            assertInstanceOf(IllegalAccessException.class, exception.getCause());
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
}
