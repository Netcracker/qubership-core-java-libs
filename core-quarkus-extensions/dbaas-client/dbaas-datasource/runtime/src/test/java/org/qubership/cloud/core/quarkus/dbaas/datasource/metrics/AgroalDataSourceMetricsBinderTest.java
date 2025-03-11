package org.qubership.cloud.core.quarkus.dbaas.datasource.metrics;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.metrics.AgroalDataSourceMetricsBinder.*;

class AgroalDataSourceMetricsBinderTest {

    @Test
    void testMetricsBoundToRegistry() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgroalDataSource dataSource = Mockito.mock(AgroalDataSource.class);
        AgroalDataSourceMetrics metrics = Mockito.mock(AgroalDataSourceMetrics.class);
        Mockito.when(dataSource.getMetrics()).thenReturn(metrics);
        List<Tag> tags = new ArrayList<>();
        String additionalTagKey = "tag_key";
        String additionalTagValue = "tag_value";
        tags.add(Tag.of(additionalTagKey, additionalTagValue));
        String dataSourceName = "dbaas_datasource";
        AgroalDataSourceMetricsBinder metricsBinder = new AgroalDataSourceMetricsBinder(dataSource, dataSourceName, tags);
        metricsBinder.bindTo(meterRegistry);
        List<Meter> meters = meterRegistry.getMeters();
        Assertions.assertTrue(meters.stream().allMatch(meter -> meter.getId().getTag("name").equals(dataSourceName)
                && meter.getId().getTag(additionalTagKey).equals(additionalTagValue)));

        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(ACTIVE_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(MAX_USED_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(AWAITING_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(ACQUIRE_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(CREATION_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(LEAK_DETECTION_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(DESTROY_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(FLUSH_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(INVALID_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(REAP_COUNT_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(BLOCKING_TIME_AVERAGE_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(BLOCKING_TIME_MAX_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(BLOCKING_TIME_TOTAL_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(CREATION_TIME_AVERAGE_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(CREATION_TIME_MAX_METRIC_NAME)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getName().equals(CREATION_TIME_TOTAL_METRIC_NAME)));
    }
}
