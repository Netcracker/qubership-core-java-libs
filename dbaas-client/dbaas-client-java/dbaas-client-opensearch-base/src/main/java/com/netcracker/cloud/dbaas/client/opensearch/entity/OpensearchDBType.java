package com.netcracker.cloud.dbaas.client.opensearch.entity;

import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;

import static com.netcracker.cloud.dbaas.client.entity.database.type.PhysicalDbType.OPENSEARCH;

public class OpensearchDBType extends DatabaseType<OpensearchIndexConnection, OpensearchIndex> {

    public static final OpensearchDBType INSTANCE = new OpensearchDBType();

    private OpensearchDBType() {
        super(OPENSEARCH, OpensearchIndex.class);
    }
}

