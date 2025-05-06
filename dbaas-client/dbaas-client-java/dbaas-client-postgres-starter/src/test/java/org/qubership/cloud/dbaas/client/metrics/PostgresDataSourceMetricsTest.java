package org.qubership.cloud.dbaas.client.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.config.metrics.PostgresMetricsConfiguration;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.TenantDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.testconfiguration.TestMicrometerConfiguration;
import org.qubership.cloud.dbaas.client.testconfiguration.TestPostgresConfig;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@SpringBootTest(properties = {"dbaas.postgres.datasource.initializationFailTimeout=-1"})
@ContextConfiguration(classes = {TestMicrometerConfiguration.class, TestPostgresConfig.class, PostgresMetricsConfiguration.class})
class PostgresDataSourceMetricsTest {
    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private DatabasePool databasePool;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testMetricBeansLoaded() {
        Assertions.assertNotNull(applicationContext.getBean(DataSourcePoolMetadataProvider.class));
        Assertions.assertNotNull(applicationContext.getBean(PostgresMetricsProvider.class));
    }

    @Test
    void testMetricsRegisteredForServiceDataSource() {
        DbaasDbClassifier classifier = new ServiceDbaaSClassifierBuilder(null).build();
        PostgresDatabase database = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, classifier);
        List<Meter> meters = meterRegistry.getMeters();
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(database.getName())
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(SERVICE)));
    }

    @Test
    void testMetricsRegisteredForTenantDataSources() {
        String firstTenant = "first_tenant";
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(firstTenant));
        DbaasDbClassifier firstTenantClassifier = new TenantDbaaSClassifierBuilder(null).build();
        PostgresDatabase firstDatabase = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, firstTenantClassifier);

        String secondTenant = "second_tenant";
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(secondTenant));
        DbaasDbClassifier secondTenantClassifier = new TenantDbaaSClassifierBuilder(null).build();
        PostgresDatabase secondDatabase = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, secondTenantClassifier);

        List<Meter> meters = meterRegistry.getMeters();
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(firstDatabase.getName())
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(firstTenant)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));
        Assertions.assertTrue(meters.stream().anyMatch(meter -> meter.getId().getTag("name").equals(secondDatabase.getName())
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + TENANT_ID).equals(secondTenant)
                && meter.getId().getTag(CLASSIFIER_TAG_PREFIX + SCOPE).equals(TENANT)));
    }
}
