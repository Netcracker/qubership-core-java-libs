package com.netcracker.cloud.dbaas.client.opensearch.service;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;

import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;

public class OpensearchLogicalDbProviderImpl extends OpensearchLogicalDbProvider {

    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PASSWORD = "pwd";

    static final String HOST = "localhost";
    static final boolean TLS = false;

    static final String RESOURCE_PREFIX = "test";
    static final int PORT = 9200;

    @Override
    public OpensearchConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        OpensearchConnectionProperty opensearchConnectionProperty = new OpensearchConnectionProperty(
                URL,
                USERNAME,
                PASSWORD,
                HOST,
                RESOURCE_PREFIX,
                PORT
        );
        opensearchConnectionProperty.setTls(false);
        opensearchConnectionProperty.setRole(ADMIN_ROLE);
        return opensearchConnectionProperty;
    }

    @Override
    public void provideDatabaseInfo(OpensearchIndex database) {
        database.setName("my-index");
    }
}
