package com.netcracker.cloud.dbaas.client.cassandra.service;

import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;

public class CassandraLogicalDbProviderImpl extends CassandraLogicalDbProvider {

    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PWD = "pwd";
    static final List<String> CONTRACT_POINTS = Collections.singletonList("contractPoints");
    static final String KEYSPACE = "keyspace";
    static final int PORT = 5433;
    static final boolean IS_TLS_MODE = false;

    @Override
    public CassandraConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        return new CassandraConnectionProperty(
                URL,
                USERNAME,
                PWD,
                CONTRACT_POINTS,
                KEYSPACE,
                PORT,
                ADMIN_ROLE,
                IS_TLS_MODE
        );
    }

    @Override
    public void provideDatabaseInfo(CassandraDatabase database) {
        database.setName("test-cassandra");
    }
}
