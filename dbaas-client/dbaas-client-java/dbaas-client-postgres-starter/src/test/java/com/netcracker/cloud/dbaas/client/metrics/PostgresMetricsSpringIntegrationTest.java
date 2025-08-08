package com.netcracker.cloud.dbaas.client.metrics;

import org.qubership.cloud.dbaas.client.testconfiguration.PostgresTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.testconfiguration.TestPostgresWithDatasourceConfig;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@EnableAutoConfiguration
@SpringBootTest(properties = {
        "cloud.microservice.name=test-app",
        "cloud.microservice.namespace=default",
        "spring.flyway.baselineOnMigrate=true"})
@ContextConfiguration(classes = {TestPostgresWithDatasourceConfig.class, PostgresTestContainerConfiguration.class})
class PostgresMetricsSpringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testMetricBeansLoaded() {
        Assertions.assertNotNull(applicationContext.getBean(MeterRegistry.class));
        Assertions.assertNotNull(applicationContext.getBean(PostgresMetricsProvider.class));
    }
}
