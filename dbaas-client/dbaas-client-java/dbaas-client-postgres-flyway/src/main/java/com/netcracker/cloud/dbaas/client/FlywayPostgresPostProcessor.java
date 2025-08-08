package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.qubership.cloud.dbaas.client.DbaasConst.CUSTOM_KEYS;
import static org.qubership.cloud.dbaas.client.DbaasConst.LOGICAL_DB_NAME;
import static org.qubership.cloud.dbaas.client.FlywayPostgresPostProcessor.FLYWAY_POSTGRES_POST_PROCESSOR_ORDER;

@Slf4j
@Order(FLYWAY_POSTGRES_POST_PROCESSOR_ORDER)
public class FlywayPostgresPostProcessor implements PostConnectProcessor<PostgresDatabase> {

    public static final int FLYWAY_POSTGRES_POST_PROCESSOR_ORDER = 100;
    public static final String CLASSPATH_DB_MIGRATION_POSTGRES = "classpath:db/migration/postgresql";
    public static final String FLYWAY_PREFIX = "flyway.";
    public static final String LOCATIONS_PROPERTY = "locations";

    private final FlywayConfigurationProperties properties;

    public FlywayPostgresPostProcessor(FlywayConfigurationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void process(PostgresDatabase postgresDatabase) {
        String databaseName = postgresDatabase.getName();
        log.info("Starting Flyway migration for database: {}", databaseName);

        FluentConfiguration configure = null;
        if (postgresDatabase.isClassifierContainsLogicalDbName()) {
            Map<String, Object> customKeys = (Map<String, Object>) postgresDatabase.getClassifier().get(CUSTOM_KEYS);
            String logicalDbName = (String) customKeys.get(LOGICAL_DB_NAME);
            FlywayConfigurationProperties.Datasource flywayProperties = properties.getDatasources().get(logicalDbName);
            configure = configureFlyway(postgresDatabase.getConnectionProperties().getDataSource(), flywayProperties, logicalDbName);
        } else {
            String sharedDbName = "static";
            FlywayConfigurationProperties.Datasource flywayProperties = properties.getDatasource();
            configure = configureFlyway(postgresDatabase.getConnectionProperties().getDataSource(), flywayProperties, sharedDbName);
        }

        configure.load().migrate();
        log.info("Finished Flyway migration for database: {}", databaseName);
    }

    private FluentConfiguration configureFlyway(DataSource dataSource, FlywayConfigurationProperties.Datasource flywayProperties, String logicalDbName) {
        Map<String, String> flywayPropertiesForDatasource = flywayProperties != null ? flywayProperties.getFlyway() : new HashMap<>();
        FluentConfiguration configure = bindInitialFlywayConfiguration(flywayPropertiesForDatasource, dataSource);
        if (!flywayPropertiesForDatasource.containsKey(LOCATIONS_PROPERTY)) {
            configure = configure.locations(CLASSPATH_DB_MIGRATION_POSTGRES + "/" + logicalDbName);
        }
        return configure;
    }

    private FluentConfiguration bindInitialFlywayConfiguration(Map<String, String> properties, DataSource dataSource) {
        Map<String, String> modifiedProperties = enrichPropertiesMapKey(properties);
        Properties propertiesWrapper = new Properties();
        propertiesWrapper.putAll(modifiedProperties);
        FluentConfiguration configure = Flyway.configure().configuration(propertiesWrapper);
        configure.dataSource(dataSource);
        return configure;
    }

    private Map<String, String> enrichPropertiesMapKey(Map<String, String> properties) {
        Map<String, String> modifiedProperties = new HashMap<>();
        properties.forEach((key, value) -> {
            String updatedKey = FLYWAY_PREFIX + key;
            modifiedProperties.put(updatedKey, value);
        });
        return modifiedProperties;
    }

    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }
}
