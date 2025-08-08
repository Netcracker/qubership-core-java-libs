package com.netcracker.cloud.dbaas.client.it.cassandra.config;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.service.DbaasCqlSessionBuilderCustomizer;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasCassandra;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.Priority;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
@EnableDbaasCassandra
public class CassandraTestConfiguration {

    @Autowired
    CassandraTestContainer container;

    @Bean
    public CassandraTestContainer container() {
        container = CassandraTestContainer.getInstance();
        container.start();
        return container;
    }

    @Bean
    @Primary
    public DbaasClient dbaasClient() throws Exception {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(DatabaseType.class), any(String.class), any(Map.class), any(DatabaseConfig.class)))
                .thenAnswer((Answer<CassandraDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    String databaseName = "test_service";
                    if (classifierFromMock.get(SCOPE).equals(TENANT)) {
                        databaseName = classifierFromMock.get(TENANT_ID);
                    }
                    return getCassandraDb(databaseName);
                });

        return dbaasClient;
    }

    public CassandraDatabase getCassandraDb(String dbName) {

        CassandraDatabase database = new CassandraDatabase();
        database.setName(dbName);

        CassandraDBConnection connection = new CassandraDBConnection();
        connection.setContactPoints(Collections.singletonList(container.getContainerIpAddress()));
        connection.setKeyspace(dbName);
        connection.setUsername("cassandra");
        connection.setPassword(null);
        connection.setPort(container.getMappedPort(9042));
        database.setConnectionProperties(connection);

        Cluster cluster = container.getCluster();
        try (Session session = cluster.connect()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS " + dbName + " WITH replication = \n" +
                    "{'class':'SimpleStrategy','replication_factor':'1'};");
        }

        return database;
    }

    @Bean
    public DbaasCqlSessionBuilderCustomizer getDbaasCqlSessionBuilderCustomizer1() {
        return new Test1DbaasCqlSessionBuilderCustomizer();
    }

    @Bean
    public DbaasCqlSessionBuilderCustomizer getDbaasCqlSessionBuilderCustomizer2() {
        return new Test2DbaasCqlSessionBuilderCustomizer();
    }

    @Priority(3)
    static public class Test1DbaasCqlSessionBuilderCustomizer implements DbaasCqlSessionBuilderCustomizer {

        @Override
        public void customize(CqlSessionBuilder cqlSessionBuilder) {

        }

        @Override
        public void customize(ProgrammaticDriverConfigLoaderBuilder programmaticDriverConfigLoaderBuilder) {
            assertEquals(Duration.ofMinutes(3), programmaticDriverConfigLoaderBuilder.build()
                    .getInitialConfig().getDefaultProfile()
                    .getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT));

            programmaticDriverConfigLoaderBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMinutes(5));
        }
    }

    @Priority(5)
    static public class Test2DbaasCqlSessionBuilderCustomizer implements DbaasCqlSessionBuilderCustomizer {

        @Override
        public void customize(CqlSessionBuilder cqlSessionBuilder) {

        }

        @Override
        public void customize(ProgrammaticDriverConfigLoaderBuilder programmaticDriverConfigLoaderBuilder) {
            assertEquals(Duration.ofMinutes(5), programmaticDriverConfigLoaderBuilder.build()
                    .getInitialConfig().getDefaultProfile()
                    .getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT));

            programmaticDriverConfigLoaderBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMinutes(50));
        }
    }
}
