package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;

@Data
public class TestDBConnection extends DatabaseConnection {
    @Override
    public void close() {}
}
