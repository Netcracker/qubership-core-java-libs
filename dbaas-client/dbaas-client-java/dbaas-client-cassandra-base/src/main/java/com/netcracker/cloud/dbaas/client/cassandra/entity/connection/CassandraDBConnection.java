package com.netcracker.cloud.dbaas.client.cassandra.entity.connection;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString(of = {"keyspace", "contactPoints", "port"}, callSuper = true)
public class CassandraDBConnection extends DatabaseConnection {
    private String keyspace;
    private List<String> contactPoints;
    private int port;

    private CqlSession session;

    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
    }
}
