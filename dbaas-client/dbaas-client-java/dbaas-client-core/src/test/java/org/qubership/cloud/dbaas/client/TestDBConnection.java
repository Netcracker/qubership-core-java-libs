package org.qubership.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;

@Data
public class TestDBConnection extends DatabaseConnection {
    @Override
    public void close() {}
}
