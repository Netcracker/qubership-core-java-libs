package com.netcracker.cloud.dbaas.client.entity.test;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;

public class TestDBType extends DatabaseType<TestDBConnection, TestDatabase> {
    public static final TestDBType INSTANCE = new TestDBType();

    private TestDBType() {
        super("testdb", TestDatabase.class);
    }
}
