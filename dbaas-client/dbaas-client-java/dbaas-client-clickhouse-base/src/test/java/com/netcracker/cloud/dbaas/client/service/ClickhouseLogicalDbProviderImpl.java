package com.netcracker.cloud.dbaas.client.service;

import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;

import java.util.SortedMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;

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
