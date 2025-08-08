package com.netcracker.cloud.dbaas.client.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.config.container.CassandraContainerIntegrationConfiguration;
import org.qubership.cloud.dbaas.client.config.container.CassandraContainerLogicalDbProvider;
import org.qubership.cloud.dbaas.client.config.container.CassandraTestContainer;
import org.qubership.cloud.dbaas.client.config.container.CassandraTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@EnableServiceDbaasCassandra
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CassandraContainerIntegrationConfiguration.class, CassandraTestContainerConfiguration.class, CassandraMigrationTest.LogicalProviderTestConfig.class},
        properties = {
                "cloud.microservice.name=test-app",
                "cloud.microservice.namespace=default"
        })
@ContextConfiguration(classes = TestConfig.class)
class CassandraMigrationTest {
    @Autowired
    DatabasePool databasePool;

    @Test
    void testMigration() {
        DbaasDbClassifier classifier = new ServiceDbaaSClassifierBuilder(null).build();
        CassandraDatabase cassandraDatabase = databasePool.getOrCreateDatabase(CassandraDBType.INSTANCE, classifier);

        CqlSession cqlSession = cassandraDatabase.getConnectionProperties().getSession();
        ResultSet resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        List<String> tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        Assertions.assertTrue(tables.containsAll(List.of("sample_migration_table_1", "sample_migration_table_2")));
    }

    @Configuration
    public static class LogicalProviderTestConfig {
        @Primary
        @Bean
        public static CassandraContainerLogicalDbProvider logicalDbProvider(CassandraTestContainer container) {
            return new CassandraContainerLogicalDbProvider(container);
        }
    }
}
