package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.config.DbaasPostgresDataSourceProperties;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection.*;
import static org.qubership.cloud.dbaas.client.metrics.PostgresMetricsProvider.DATASOURCE_PARAMETER;
import static org.qubership.cloud.dbaas.client.metrics.PostgresMetricsProvider.SCHEMA_TAG;

@Slf4j
public class PostgresDatasourceCreator implements DatabaseClientCreator<PostgresDatabase, DatasourceConnectorSettings> {
    private DbaasPostgresDataSourceProperties dbaasDsProperties;
    private DbaaSMetricsRegistrar metricsRegistrar;

    public PostgresDatasourceCreator(DbaasPostgresDataSourceProperties dbaasDsProperties,
                                     DbaaSMetricsRegistrar metricsRegistrar) {
        this.dbaasDsProperties = dbaasDsProperties;
        this.metricsRegistrar = metricsRegistrar;
    }

    @Override
    public void create(PostgresDatabase database) {
        create(database, new DatasourceConnectorSettings());
    }

    @Override
    public void create(PostgresDatabase database, DatasourceConnectorSettings settings) {
        if (settings == null) {
            settings = new DatasourceConnectorSettings();
        }
        log.debug("Starting the initialization of DataSource for database: {}", database);
        PostgresDBConnection connectionProperties = database.getConnectionProperties();
        Class<?> dataSourceType = dbaasDsProperties.getDataSourceType();
        Class<? extends DataSource> dsType;
        log.debug("dataSourceType from properties: {}", dataSourceType);
        if (dataSourceType == null) {
            dsType = HikariDataSource.class;
        } else {
            if (!DataSource.class.isAssignableFrom(dataSourceType)) {
                throw new IllegalArgumentException("Specified datasource class: " + dataSourceType.getName() + " is not valid! Must implement " + DataSource.class.getName());
            }
            dsType = (Class<? extends DataSource>) dataSourceType;
        }
        log.debug("Using dataSourceType: {}", dsType);

        String url = connectionProperties.getUrl();
        if (settings.isRoReplica()) {
            if (StringUtils.hasText(connectionProperties.getRoHost())) {
                url = url.replace(connectionProperties.getHost(), connectionProperties.getRoHost());
            } else {
                throw new IllegalArgumentException("Requested a connection to the ro replice, but the ro replica is missing from the connection properties.");
            }
        }
        if (connectionProperties.isTls()) {
            log.info("Connection to postgresql will be secured");
            if(connectionProperties.isTlsNotStrict())
                url = appendUrlParam(url, SSL_MODE_REQUIRE_POSTFIX);
            else
                url = appendUrlParam(url, SSL_MODE_VERIFY_FULL_POSTFIX);
            url = appendUrlParam(url, SSL_FACTORY_POSTFIX);
        }
        String username = connectionProperties.getUsername();
        String password = connectionProperties.getPassword();

        password = connectionProperties.getPassword();
        log.debug("use password from dbaas storage");


        DataSource dataSource;
        if (dsType == HikariDataSource.class) {
            Properties connProperties = new Properties();
            Map<Object, Object> datasourceProps = new HashMap<>(this.dbaasDsProperties.getDatasource());
            datasourceProps.put("dataSourceProperties", buildProperties((String) datasourceProps.remove("connection-properties"), ";"));
            connProperties.putAll(datasourceProps);
            if (!CollectionUtils.isEmpty(settings.getConnPropertiesParam())) {
                connProperties.putAll(settings.getConnPropertiesParam());
            }
            connProperties.setProperty("username", username);
            connProperties.setProperty("password", password);
            connProperties.setProperty("jdbcUrl", url);
            HikariConfig hikariConfig = new HikariConfig(connProperties);

            if (StringUtils.hasText(settings.getSchema())) {
                hikariConfig.setSchema(settings.getSchema());
            }
            dataSource = new HikariDataSource(hikariConfig);
        } else {
            DataSourceBuilder<? extends DataSource> dataSourceBuilder = DataSourceBuilder.create(this.getClass().getClassLoader())
                    .type(dsType)
                    .url(url)
                    .username(connectionProperties.getUsername())
                    .password(password);

            dataSource = dataSourceBuilder.build();

            ConfigurationPropertySource source = new MapConfigurationPropertySource(this.dbaasDsProperties.getDatasource());
            ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
            aliases.addAliases("url", "jdbc-url");
            aliases.addAliases("username", "user");
            aliases.addAliases("connection-properties", "data-source-properties");
            Binder binder = new Binder(source.withAliases(aliases));
            log.debug("Applying datasource properties: {}", this.dbaasDsProperties.getDatasource());
            binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(dataSource));
        }

        if (!AutoCloseable.class.isAssignableFrom(dsType)) {
            log.error("Cannot correctly use provided data source class {}, because it was not derived from {}",
                    dsType.getName(), AutoCloseable.class.getName());
        }

        log.info("dataSource created for {}", database);
        connectionProperties.setDataSource(dataSource);
        if (settings.getFlywayRunner() != null) {
            log.info("going to execute flyway migrations for {}", database);
            settings.getFlywayRunner().run(new FlywayContext(dataSource));
        }
        registerMetrics(database, settings);
    }

    private String appendUrlParam(String url, String param) {
        String concatSign = "&";
        if (!url.contains("?")) {
            concatSign = "?";
        }
        return url + concatSign + param;
    }

    @SneakyThrows
    private Properties buildProperties(String propertiesFromString, String entrySeparator) {
        Properties properties = new Properties();
        properties.load(new StringReader(propertiesFromString.replaceAll(entrySeparator, "\n")));
        return properties;
    }

    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }

    private void registerMetrics(PostgresDatabase database, DatasourceConnectorSettings settings) {
        if (metricsRegistrar != null) {
            if (database.getName() == null) {
                log.warn("Database name is null");
            }
            DatabaseMetricProperties metricProperties = DatabaseMetricProperties.builder()
                    .databaseName(database.getName())
                    .role(database.getConnectionProperties().getRole())
                    .classifier(database.getClassifier())
                    .extraParameters(Map.of(DATASOURCE_PARAMETER, database.getConnectionProperties().getDataSource()))
                    .additionalTags(Map.of(SCHEMA_TAG, String.valueOf(settings.getSchema())))
                    .build();
            metricsRegistrar.registerMetrics(PostgresDBType.INSTANCE, metricProperties);
        }
    }

}
