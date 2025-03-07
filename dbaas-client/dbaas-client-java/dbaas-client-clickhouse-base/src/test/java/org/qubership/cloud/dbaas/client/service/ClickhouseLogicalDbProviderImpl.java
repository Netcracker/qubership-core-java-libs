package org.qubership.cloud.dbaas.client.service;

import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;

import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;

public class ClickhouseLogicalDbProviderImpl extends ClickhouseLogicalDbProvider {
    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PASSWORD = "pwd";

    @Override
    public ClickhouseConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        return new ClickhouseConnectionProperty(
                URL,
                USERNAME,
                PASSWORD,
                ADMIN_ROLE
        );
    }

    @Override
    public void provideDatabaseInfo(ClickhouseDatabase database) {
        database.setName("my-cl");
    }
}