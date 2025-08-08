package com.netcracker.cloud.dbaas.client.cassandra.migration.session;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.netcracker.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaAgreementSettings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;


@Slf4j
public class SchemaMigrationSession {
    @Getter
    // Use getter only in specific cases, waiting for schema-agreement required in lib
    private final CqlSession session;

    private final Long schemaAgreementAwaitRetryDelay;

    public SchemaMigrationSession(
            CqlSession cqlSession,
            SchemaAgreementSettings schemaAgreementSettings
    ) {
        this.session = cqlSession;
        this.schemaAgreementAwaitRetryDelay = schemaAgreementSettings.awaitRetryDelay();
    }

    public ResultSet execute(String interpolatedQuery) {
        return execute(SimpleStatement.newInstance(interpolatedQuery));
    }

    public ResultSet execute(Statement<?> statement) {
        Statement<?> localQuorumStatement = statement.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
        ResultSet resultSet = session.execute(localQuorumStatement);
        checkAndAwaitSchemaAgreement(resultSet);
        return resultSet;
    }

    public PreparedStatement prepare(String queryForPreparing) {
        return session.prepare(queryForPreparing);
    }

    public Optional<CqlIdentifier> getKeyspace() {
        return session.getKeyspace();
    }

    public Metadata getMetadata() {
        return session.getMetadata();
    }

    private void checkAndAwaitSchemaAgreement(ResultSet resultSet) {
        if (!resultSet.getExecutionInfo().isSchemaInAgreement()) {
            awaitForAgreement();
        }
    }

    private void awaitForAgreement() {
        log.info(MIGRATION_LOG_PREFIX + "Waiting for schema agreement.");
        try {
            while (!session.checkSchemaAgreement()) {
                log.info(MIGRATION_LOG_PREFIX + "Waiting for schema agreement continue");
                Thread.sleep(schemaAgreementAwaitRetryDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchemaMigrationException("Interrupted while waiting for schema agreement");
        }
        log.info(MIGRATION_LOG_PREFIX + "Waiting for schema agreement completed.");
    }
}
