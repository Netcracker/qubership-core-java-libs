package org.qubership.cloud.core.quarkus.dbaas.datasource.config;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.JDBCConfig;
import io.agroal.api.cache.ConnectionCache;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.exceptionsorter.PostgreSQLExceptionSorter;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.runtime.configuration.ConfigInstantiator;
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
    private static final String QUARKUS_JDBC_PROPERTY_PREFIX = "quarkus.j-d-bc.";
    public static final String JDBC_PROPERTY_PREFIX = "jdbc.";

    public AgroalConnectionPoolConfiguration createAgroalConnectionPoolConfiguration(String logicaldb, Map<String, Object> customparams) {
        JDBCConfig effectiveJdbcConfig = null;
        if (customparams != null && !customparams.isEmpty()) {
            effectiveJdbcConfig = buildJdbcConfigFromProperties(customparams);
        }
        if (effectiveJdbcConfig == null && logicaldb != null) {
            effectiveJdbcConfig = Optional.ofNullable(datasourceProperties.datasources)
                    .map(datasources -> datasources.get(logicaldb))
                    .map(jdbcProperties -> jdbcProperties.jdbc)
                    .orElse(null);
        }
        if (effectiveJdbcConfig == null) {
            effectiveJdbcConfig = datasourceProperties.jdbc;
        }
        return new AgroalConnectionPoolConfigurationSupplier()
                .validationTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceValidationInterval)))
                .idleValidationTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceIdleValidationTimeout)))
                .reapTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceReapTimeout)))
                .acquisitionTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceAcquisitionTimeout)))
                .leakTimeout(Duration.ofMillis((long) (1000 * effectiveJdbcConfig.datasourceLeakDetectionInterval)))
                .connectionValidator(new DbaasDatasourcePoolConfiguration.DbConnectionValidator(Integer.parseInt(effectiveJdbcConfig.datasourceRespondTimeToDrop)))
                .maxSize(effectiveJdbcConfig.poolSize)
                .minSize(effectiveJdbcConfig.minPoolSize)
                .initialSize(effectiveJdbcConfig.initPoolSize)
                .enhancedLeakReport(datasourceProperties.enhancedLeakReport)
                .flushOnClose(effectiveJdbcConfig.flushOnClose)
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
                String newKey = QUARKUS_JDBC_PROPERTY_PREFIX + key.substring(JDBC_PROPERTY_PREFIX.length());
                jdbcProperties.put(newKey, value.toString());
            }
        });
        if (!jdbcProperties.isEmpty()) {
            JDBCConfig jdbcConfig = new JDBCConfig();
            SmallRyeConfig config = new SmallRyeConfigBuilder().withDefaultValues(jdbcProperties).build();
            ConfigInstantiator.handleObject(jdbcConfig, config);
            return jdbcConfig;
        }
        return null;
    }
}
