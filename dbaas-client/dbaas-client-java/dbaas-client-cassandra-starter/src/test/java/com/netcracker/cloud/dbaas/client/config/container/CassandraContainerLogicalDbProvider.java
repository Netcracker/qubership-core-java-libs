package com.netcracker.cloud.dbaas.client.config.container;

import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.qubership.cloud.dbaas.client.config.container.CassandraContainerIntegrationConfiguration.*;

@AllArgsConstructor
public class CassandraContainerLogicalDbProvider implements LogicalDbProvider<CassandraDBConnection, CassandraDatabase> {

    private CassandraTestContainer container;

    @Override
    public Class<CassandraDatabase> getSupportedDatabaseType() {
        return CassandraDatabase.class;
    }

    @Override
    public CassandraDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        CassandraDatabase cassandraDatabase = new CassandraDatabase();
        String keyspace;
        if (TENANT.equals(classifier.get(SCOPE))) {
            keyspace = (String) classifier.get(TENANT_ID);
        } else {
            keyspace = SERVICE_KEYSPACE;
        }
        cassandraDatabase.setName(keyspace);
        cassandraDatabase.setConnectionProperties(provideConnectionProperty(classifier, params));
        cassandraDatabase.setClassifier(new TreeMap<>(classifier));
        return cassandraDatabase;
    }

    @Override
    public CassandraDBConnection provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        InetSocketAddress contactPoint = container.getContactPoint();
        cassandraDBConnection.setContactPoints(List.of(contactPoint.getHostString()));
        cassandraDBConnection.setPort(contactPoint.getPort());
        cassandraDBConnection.setUsername(container.getUsername());
        cassandraDBConnection.setPassword(container.getPassword());

        cassandraDBConnection.setRole(TEST_ROLE);

        String keyspace;
        if (TENANT.equals(classifier.get(SCOPE))) {
            keyspace = (String) classifier.get(TENANT_ID);
        } else {
            keyspace = SERVICE_KEYSPACE;
        }
        cassandraDBConnection.setKeyspace(keyspace);
        return cassandraDBConnection;
    }
}
