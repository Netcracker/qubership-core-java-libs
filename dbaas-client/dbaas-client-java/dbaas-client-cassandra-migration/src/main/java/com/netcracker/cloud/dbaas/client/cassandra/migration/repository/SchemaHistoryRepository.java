package com.netcracker.cloud.dbaas.client.cassandra.migration.repository;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.SchemaVersionFromDb;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import com.netcracker.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.List;
import java.util.stream.Collectors;

public class SchemaHistoryRepository extends AbstractCqlRepository {
    private static final String SELECT_SCHEMA_HISTORY_CQL = "select version, checksum, success, installed_rank, description from %s";
    private static final String INSERT_SCHEMA_HISTORY_CQL = "insert into %s (" +
            "installed_rank, version, description, type, script, checksum, installed_by, installed_on, success, extra_info) " +
            "values (?, ?, ?, ?, ?, ?, ?, toTimestamp(now()), ?, ?)";

    private static final String UPDATE_SCHEMA_HISTORY_CQL = "update %s set execution_time = ?, success = ?, " +
            "extra_info = ? where installed_rank = ?";

    private final String tableName;
    private final PreparedStatement insertSchemaHistoryPs;
    private final PreparedStatement updateSchemaHistoryPs;

    public SchemaHistoryRepository(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        super(session);
        this.tableName = schemaMigrationSettings.schemaHistoryTableName();
        this.insertSchemaHistoryPs = session.prepare(String.format(INSERT_SCHEMA_HISTORY_CQL, tableName));
        this.updateSchemaHistoryPs = session.prepare(String.format(UPDATE_SCHEMA_HISTORY_CQL, tableName));
    }

    public void insert(
            int installedRank, String version, String description, String type, String script,
            long checksum, String installedBy, boolean success, String extraInfo
    ) {
        session.execute(insertSchemaHistoryPs.bind(
                installedRank, version, description, type, script, checksum, installedBy, success, extraInfo
        ));
    }

    public void updateExecutionInfo(
            int installedRank, long executionTime, boolean success, String extraInfo
    ) {
        session.execute(
                updateSchemaHistoryPs.bind(executionTime, success, extraInfo, installedRank)
        );
    }

    //Not using PreparedStatement since expected to be executed only few times
    public List<SchemaVersionFromDb> selectAll() {
        return session.execute(
                        String.format(SELECT_SCHEMA_HISTORY_CQL, tableName)
                )
                .all().stream()
                .map(row -> new SchemaVersionFromDb(
                        new ComparableVersion(row.getString("version")),
                        row.getLong("checksum"),
                        row.getBoolean("success"),
                        row.getInt("installed_rank"),
                        row.getString("description")
                ))
                .collect(Collectors.toList());
    }
}
