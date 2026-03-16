package com.netcracker.cloud.dbaas.client.opensearch.config.metrics;

import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({
    MetricsAutoConfiguration.class,
    SimpleMetricsExportAutoConfiguration.class})
@Configuration
public class TestMicrometerConfiguration {
}
