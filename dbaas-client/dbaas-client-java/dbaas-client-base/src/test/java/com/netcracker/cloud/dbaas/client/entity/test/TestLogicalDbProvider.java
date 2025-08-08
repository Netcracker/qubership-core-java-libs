package com.netcracker.cloud.dbaas.client.entity.test;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.service.LogicalDbProvider;

import java.util.SortedMap;

public class TestLogicalDbProvider implements LogicalDbProvider<Void, TestDatabase> {
    private DbaasClient dbaasClient;

    public TestLogicalDbProvider(DbaasClient dbaasClient) {
        this.dbaasClient = dbaasClient;
    }

    @Override
    public TestDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
        return dbaasClient.getOrCreateDatabase(
                TestDBType.INSTANCE,
                namespace,
                classifier,
                params
        );
    }

    @Override
    public Void provideConnectionProperty(SortedMap classifier, DatabaseConfig params) {
        return null;
    }

    @Override
    public Class<TestDatabase> getSupportedDatabaseType() {
        return TestDatabase.class;
    }

}
