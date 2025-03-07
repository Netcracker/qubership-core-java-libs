package org.qubership.cloud.dbaas.client.opensearch.entity;

import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;

import static org.qubership.cloud.dbaas.client.entity.database.type.PhysicalDbType.OPENSEARCH;

public class OpensearchDBType extends DatabaseType<OpensearchIndexConnection, OpensearchIndex> {

    public static final OpensearchDBType INSTANCE = new OpensearchDBType();

    private OpensearchDBType() {
        super(OPENSEARCH, OpensearchIndex.class);
    }
}

