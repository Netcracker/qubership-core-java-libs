package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.CassandraDbaaSSessionProxy;
import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;

import static com.netcracker.cloud.dbaas.client.config.DbaasCassandraConfiguration.TENANT_CASSANDRA_TEMPLATE;

@Configuration
@Import(DbaasCassandraConfiguration.class)
@Slf4j
public class DbaasTenantCassandraConfiguration {

        @Bean(name = { TENANT_CASSANDRA_TEMPLATE })
        @ConditionalOnMissingBean(name = TENANT_CASSANDRA_TEMPLATE)
        public CassandraTemplate tenantCassandraTemplate(@Autowired DatabasePool pool,
                        DbaasClassifierFactory classifierFactory,
                        CassandraConverter converter,
                        DbaasApiProperties cassandraDbaasApiProperties,
                        DbaasCassandraProperties dbaasCassandraProperties) {
                log.debug("Start initialize tenant cassandra template {} bean", TENANT_CASSANDRA_TEMPLATE);
                DatabaseConfig databaseConfig = DatabaseConfig.builder()
                                .dbNamePrefix(cassandraDbaasApiProperties.getDbPrefix())
                                .userRole(cassandraDbaasApiProperties.getRuntimeUserRole())
                                .build();
                CassandraTemplate template = new CassandraTemplate(
                                new CassandraDbaaSSessionProxy(pool, classifierFactory.newTenantClassifierBuilder(),
                                                databaseConfig),
                                converter);
                template.setUsePreparedStatements(dbaasCassandraProperties.isUsePreparedStatements());
                return template;
        }
}
