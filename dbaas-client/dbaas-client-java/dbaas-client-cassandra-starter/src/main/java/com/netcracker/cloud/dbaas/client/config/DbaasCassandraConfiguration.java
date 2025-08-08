package com.netcracker.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import org.qubership.cloud.dbaas.client.cassandra.service.DbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.cassandra.service.DefaultDbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.CassandraPostConnectProcessor;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@EnableDbaasDefault
@Slf4j
@EnableConfigurationProperties
@Import(DbaasCassandraMigrationConfiguration.class)
public class DbaasCassandraConfiguration {
    public static final String SERVICE_CASSANDRA_TEMPLATE = "serviceCassandraTemplate";
    public static final String TENANT_CASSANDRA_TEMPLATE = "tenantCassandraTemplate";

    @Bean
    @ConditionalOnMissingBean
    public CassandraMappingContext mappingContext() {
        return new CassandraMappingContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public CassandraConverter converter(@Autowired CassandraMappingContext mappingContext) {
        return new MappingCassandraConverter(mappingContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public CassandraSessionBuilder dbaasCassandraSessionBuilder(
            ObjectProvider<DbaasCqlSessionBuilderCustomizer> cqlSessionBuilderCustomizers,
            @Autowired(required = false) DbaaSMetricsRegistrar metricsRegistrar) {
        return new CassandraSessionBuilder(getSortedList(cqlSessionBuilderCustomizers),
                metricsRegistrar);
    }

    private List<DbaasCqlSessionBuilderCustomizer> getSortedList(ObjectProvider<DbaasCqlSessionBuilderCustomizer> cqlSessionBuilderCustomizers) {
        return cqlSessionBuilderCustomizers.orderedStream().collect(Collectors.toList());
    }

    @Bean
    @ConfigurationProperties("dbaas.cassandra")
    public DbaasCassandraProperties dbaasCassandraProperties() {
        return new DbaasCassandraProperties();
    }

    @Bean
    public DbaasCqlSessionBuilderCustomizer defaultDbaasCqlSessionBuilderCustomizer(DbaasCassandraProperties dbaasCassandraProperties) {
        return new DefaultDbaasCqlSessionBuilderCustomizer(dbaasCassandraProperties);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public PostConnectProcessor<CassandraDatabase> cassandraDatabasePostConnectProcessor(
            CassandraSessionBuilder dbaasCassandraSessionBuilder,
            @Autowired(required = false) MigrationExecutor migrationExecutor) {
        return new CassandraPostConnectProcessor(dbaasCassandraSessionBuilder, migrationExecutor);
    }

    @Bean("cassandraDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.cassandra")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }

}
