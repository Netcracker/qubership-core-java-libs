package com.netcracker.cloud.dbaas.client.cassandra.it.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.qubership.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.it.CassandraTestContainer;
import org.qubership.cloud.dbaas.client.cassandra.metrics.CassandraMetricsProvider;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutorImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReader;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.SchemaVersionResourceReaderImpl;
import org.qubership.cloud.dbaas.client.cassandra.migration.service.resource.SchemaVersionResourceFinderRegistry;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraSessionBuilder;
import org.qubership.cloud.dbaas.client.cassandra.service.DefaultDbaasCqlSessionBuilderCustomizer;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;

class CassandraMigrationTest {

    static final String TEST_ROLE = "test-role";
    static final String SERVICE_KEYSPACE = "service_db";

    @BeforeAll
    static void setUp() {
        CassandraTestContainer.getInstance().start();
    }

    @AfterAll
    static void tearDown() {
        if (CassandraTestContainer.getInstance().isRunning()) {
            CassandraTestContainer.getInstance().stop();
        }
    }

    @Test
    void testMigrationExecutor() {
        DbaaSMetricsRegistrar dbaaSMetricsRegistrar = new DbaaSMetricsRegistrar(Collections.singletonList(new CassandraMetricsProvider()));
        DefaultDbaasCqlSessionBuilderCustomizer customizer = new DefaultDbaasCqlSessionBuilderCustomizer(new DbaasCassandraProperties());
        CassandraSessionBuilder cassandraSessionBuilder = new CassandraSessionBuilder(Collections.singletonList(customizer), dbaaSMetricsRegistrar);

        SortedMap<String, Object> serviceClassifier = new TreeMap<>();
        serviceClassifier.put(SCOPE, SERVICE);
        CqlSession cqlSession = cassandraSessionBuilder.build(getCassandraDatabase(serviceClassifier, SERVICE_KEYSPACE));

        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder().build();
        SchemaVersionResourceReader schemaVersionResourceReader = new SchemaVersionResourceReaderImpl(schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry());
        MigrationExecutor migrationExecutor = new MigrationExecutorImpl(schemaMigrationSettings, schemaVersionResourceReader, null);
        migrationExecutor.migrate(cqlSession);

        ResultSet resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        List<String> tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        Assertions.assertTrue(tables.containsAll(List.of("table0", "table1", "table2", "table3", "table4", "table5", "table6", "table7", "table8")));
    }

    @Test
    void testRepeatedMigration() {
        DbaaSMetricsRegistrar dbaaSMetricsRegistrar = new DbaaSMetricsRegistrar(Collections.singletonList(new CassandraMetricsProvider()));
        DefaultDbaasCqlSessionBuilderCustomizer customizer = new DefaultDbaasCqlSessionBuilderCustomizer(new DbaasCassandraProperties());

        CassandraSessionBuilder cassandraSessionBuilder = new CassandraSessionBuilder(Collections.singletonList(customizer), dbaaSMetricsRegistrar);

        SortedMap<String, Object> serviceClassifier = new TreeMap<>();
        serviceClassifier.put(SCOPE, SERVICE);
        CqlSession cqlSession = cassandraSessionBuilder.build(getCassandraDatabase(serviceClassifier, SERVICE_KEYSPACE));

        ResultSet resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        List<String> tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        List<String> tablesToCheck = List.of("table0", "table1", "table2", "table3", "table4", "table5", "table6", "table7", "table8");
        Assertions.assertTrue(tables.stream().noneMatch(s -> tablesToCheck.contains(s)));

        SchemaMigrationSettings schemaMigrationSettings = SchemaMigrationSettings.builder().build();
        SchemaVersionResourceReader schemaVersionResourceReader = new SchemaVersionResourceReaderImpl(schemaMigrationSettings.version(), new SchemaVersionResourceFinderRegistry());
        MigrationExecutor migrationExecutor = new MigrationExecutorImpl(schemaMigrationSettings, schemaVersionResourceReader, null);
        migrationExecutor.migrate(cqlSession);

        resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        Assertions.assertTrue(tables.containsAll(tablesToCheck));

        Assertions.assertDoesNotThrow(() -> migrationExecutor.migrate(cqlSession));
        resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        Assertions.assertTrue(tables.containsAll(tablesToCheck));
    }

    static CassandraDatabase getCassandraDatabase(SortedMap<String, Object> classifier, String keyspace) {
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        InetSocketAddress contactPoint = CassandraTestContainer.getInstance().getContactPoint();
        cassandraDBConnection.setContactPoints(List.of(contactPoint.getHostString()));
        cassandraDBConnection.setPort(contactPoint.getPort());
        cassandraDBConnection.setUsername(CassandraTestContainer.getInstance().getUsername());
        cassandraDBConnection.setPassword(CassandraTestContainer.getInstance().getPassword());

        cassandraDBConnection.setRole(TEST_ROLE);

        cassandraDBConnection.setKeyspace(keyspace);

        CassandraDatabase cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setName(keyspace);
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);
        cassandraDatabase.setClassifier(classifier);
        return cassandraDatabase;
    }
}
