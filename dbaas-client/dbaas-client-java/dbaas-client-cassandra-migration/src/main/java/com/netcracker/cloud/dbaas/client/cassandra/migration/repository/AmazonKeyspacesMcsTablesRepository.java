package com.netcracker.cloud.dbaas.client.cassandra.migration.repository;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

import java.util.Map;
import java.util.stream.Collectors;

public class AmazonKeyspacesMcsTablesRepository extends AbstractMetadataAwareCqlRepository {
    private static final String SYSTEM_SCHEMA_MCS_TABLES = "system_schema_mcs.tables";
    private static final String SELECT_ALL_TABLE_STATUS_FOR_KEYSPACE_CQL =
            "select table_name, status from " + SYSTEM_SCHEMA_MCS_TABLES + " where keyspace_name = ?";

    private final PreparedStatement selectTableStatusForKeyspacePs;

    public AmazonKeyspacesMcsTablesRepository(SchemaMigrationSession session) {
        super(session);
        this.selectTableStatusForKeyspacePs = session.prepare(SELECT_ALL_TABLE_STATUS_FOR_KEYSPACE_CQL);
    }

    public Map<String, String> getAllTableStatuses() {
        return this.session.execute(selectTableStatusForKeyspacePs.bind(keyspaceMetadata.getName().toString())).all().stream()
                .collect(Collectors.toMap(row -> row.getString("table_name"), row -> row.getString("status")));
    }
}
