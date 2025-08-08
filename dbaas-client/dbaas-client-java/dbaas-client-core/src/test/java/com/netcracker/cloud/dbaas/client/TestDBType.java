package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;

public class TestDBType extends DatabaseType<TestDBConnection, TestDatabase>  {
    public static final TestDBType INSTANCE = new TestDBType();

    private TestDBType() {
        super("testdbtype", TestDatabase.class);
    }

}
