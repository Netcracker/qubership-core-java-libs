package com.netcracker.cloud.core.quarkus.dbaas.datasource.config;

import com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties.JDBCConfig;
import io.agroal.api.cache.ConnectionCache;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.exceptionsorter.PostgreSQLExceptionSorter;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class AgroalConnectionPoolConfigurationFactory {

    private DatasourceProperties datasourceProperties;
    private TransactionManager transactionManager;
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    public static final String JDBC_PROPERTY_PREFIX = "jdbc.";

    public AgroalConnectionPoolConfiguration createAgroalConnectionPoolConfiguration(String logicaldb, Map<String, Object> customparams) {
        JDBCConfig effectiveJdbcConfig = null;
        if (customparams != null && !customparams.isEmpty()) {
            effectiveJdbcConfig = buildJdbcConfigFromProperties(customparams);
        }
        if (effectiveJdbcConfig == null && logicaldb != null) {
            effectiveJdbcConfig = Optional.ofNullable(datasourceProperties.datasources())
                    .map(datasources -> datasources.get(logicaldb))
                    .map(DatasourceProperties.JDBCProperties::jdbc)
                    .orElse(null);
        }
        if (effectiveJdbcConfig == null) {
            effectiveJdbcConfig = datasourceProperties.jdbc();
        }
        return new AgroalConnectionPoolConfigurationSupplier()
                .validationTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceValidationInterval())))
                .idleValidationTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceIdleValidationTimeout())))
                .reapTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceReapTimeout())))
                .acquisitionTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceAcquisitionTimeout())))
                .leakTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceLeakDetectionInterval())))
                .connectionValidator(new DbaasDatasourcePoolConfiguration.DbConnectionValidator(Integer.parseInt(effectiveJdbcConfig.datasourceRespondTimeToDrop())))
                .maxSize(effectiveJdbcConfig.poolSize())
                .minSize(effectiveJdbcConfig.minPoolSize())
                .initialSize(effectiveJdbcConfig.initPoolSize())
                .enhancedLeakReport(datasourceProperties.enhancedLeakReport())
                .flushOnClose(effectiveJdbcConfig.flushOnClose())
                .transactionIntegration(new NarayanaTransactionIntegration(transactionManager, transactionSynchronizationRegistry))
                // Set LocalConnectionCache to a dummy-one (no-op)
                // Agroal (and Quarkus) uses local cache with ThreadLocal behaviour so we can face a situation when
                // we are trying to get a connection for the Tenant2 db, but the connection for the Tenant1 db is taken from the local cache.
                .connectionCache(ConnectionCache.none())
                .exceptionSorter(new PostgreSQLExceptionSorter())
                .get();
    }

    private JDBCConfig buildJdbcConfigFromProperties(Map<String, Object> customparams) {
        Map<String, String> jdbcProperties = new HashMap<>();
        customparams.forEach((key, value) -> {
            if (key.startsWith(JDBC_PROPERTY_PREFIX)) {
                jdbcProperties.put(key, value.toString());
            }
        });
        if (jdbcProperties.isEmpty()) {
            return null;
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValues(jdbcProperties)
                .withMapping(JDBCConfig.class, "jdbc")
                .build();

        return config.getConfigMapping(JDBCConfig.class, "jdbc");
    }
}
