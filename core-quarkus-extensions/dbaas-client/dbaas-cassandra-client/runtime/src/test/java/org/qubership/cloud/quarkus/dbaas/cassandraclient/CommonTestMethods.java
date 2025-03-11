package org.qubership.cloud.quarkus.dbaas.cassandraclient;

import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;

import java.util.List;

public class CommonTestMethods {
    public static CassandraDBConnection prepareCassandraDBConnection() {
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        cassandraDBConnection.setContactPoints(List.of("test-url"));
        cassandraDBConnection.setPort(9142);
        cassandraDBConnection.setKeyspace("test_keyspace");
        cassandraDBConnection.setUsername("test_user");
        cassandraDBConnection.setPassword("test_password");
        return cassandraDBConnection;
    }

}
