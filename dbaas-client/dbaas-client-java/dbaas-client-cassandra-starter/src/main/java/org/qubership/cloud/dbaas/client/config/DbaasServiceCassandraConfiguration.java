package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.CassandraDbaaSSessionProxy;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;

import lombok.extern.slf4j.Slf4j;

import static org.qubership.cloud.dbaas.client.config.DbaasCassandraConfiguration.SERVICE_CASSANDRA_TEMPLATE;

@Configuration
@Import(DbaasCassandraConfiguration.class)
@Slf4j
public class DbaasServiceCassandraConfiguration {

        @Bean(name = SERVICE_CASSANDRA_TEMPLATE)
        @ConditionalOnMissingBean(name = SERVICE_CASSANDRA_TEMPLATE)
        public CassandraTemplate serviceCassandraTemplate(@Autowired DatabasePool pool,
                        DbaasClassifierFactory classifierFactory,
                        CassandraConverter converter,
                        DbaasApiProperties cassandraDbaasApiProperties,
                        DbaasCassandraProperties dbaasCassandraProperties) {
                log.debug("Start initialize microservice cassandra template {} bean", SERVICE_CASSANDRA_TEMPLATE);
                DatabaseConfig databaseConfig = DatabaseConfig.builder()
                                .dbNamePrefix(cassandraDbaasApiProperties.getDbPrefix())
                                .userRole(cassandraDbaasApiProperties.getRuntimeUserRole())
                                .build();
                CassandraTemplate template = new CassandraTemplate(
                                new CassandraDbaaSSessionProxy(pool, classifierFactory.newServiceClassifierBuilder(),
                                                databaseConfig),
                                converter);
                template.setUsePreparedStatements(dbaasCassandraProperties.isUsePreparedStatements());
                return template;
        }
}
