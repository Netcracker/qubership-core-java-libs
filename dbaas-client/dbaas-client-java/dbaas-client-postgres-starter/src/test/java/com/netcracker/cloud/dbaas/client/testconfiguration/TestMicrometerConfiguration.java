package com.netcracker.cloud.dbaas.client.testconfiguration;


import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricsAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class})
@Configuration
public class TestMicrometerConfiguration {
}
