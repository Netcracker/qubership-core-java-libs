package com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.ak;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperation;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.operation.TableOperationType;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.ak.TableStatusCheckSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmazonKeyspacesTableStateAwaitServiceTest {
    private BoundStatement boundMcsSelectStatement;
    private SchemaMigrationSession session;
    private AmazonKeyspacesTableStateAwaitService awaitService;

    @BeforeEach
    public void setupSession() {
        String keyspaceName = "test";
        SchemaMigrationSession session = mock(SchemaMigrationSession.class);
        CqlIdentifier keyspaceIdentifier = CqlIdentifier.fromInternal(keyspaceName);
        when(session.getKeyspace()).thenReturn(Optional.of(keyspaceIdentifier));

        KeyspaceMetadata keyspaceMetadata = mock(KeyspaceMetadata.class);
        when(keyspaceMetadata.getName()).thenReturn(keyspaceIdentifier);

        Metadata metadata = mock(Metadata.class);
        when(metadata.getKeyspace(eq(keyspaceIdentifier))).thenReturn(Optional.of(keyspaceMetadata));
        when(session.getMetadata()).thenReturn(metadata);

        PreparedStatement ps = mock(PreparedStatement.class);
        when(session.prepare(startsWith("select table_name, status from system_schema_mcs.tables")))
                .thenReturn(ps);

        BoundStatement bs = mock(BoundStatement.class);
        when(ps.bind(keyspaceName)).thenReturn(bs);

        this.boundMcsSelectStatement = bs;
        this.session = session;
        this.awaitService = new AmazonKeyspacesTableStateAwaitService(
                session, new TableStatusCheckSettings(0L, 500L)
        );
    }

    private void setupDbStatusSelection(
            List<Map<String, String>> dbStatusSelectResults
    ) {
        List<ResultSet> resultSets = dbStatusSelectResults.stream()
                .map(dbStatusSelectResult -> {
                    List<Row> rows = dbStatusSelectResult.entrySet().stream()
                            .map(e -> {
                                Row r = mock(Row.class);
                                when(r.getString(eq("table_name"))).thenReturn(e.getKey());
                                when(r.getString(eq("status"))).thenReturn(e.getValue());
                                return r;
                            })
                            .toList();

                    ResultSet rs = mock(ResultSet.class);
                    when(rs.all()).thenReturn(rows);
                    return rs;
                }).toList();

        OngoingStubbing<ResultSet> stubbing = when(this.session.execute(boundMcsSelectStatement));
        for (ResultSet rs : resultSets) {
            stubbing = stubbing.thenReturn(rs);
        }
    }

    @Test
    @Timeout(5)
    void testAwait() {
        setupDbStatusSelection(List.of(
                Map.of(
                        "table1", "CREATING",
                        "table2", "UPDATING",
                        "table3", "DROPPING"
                ),
                Map.of(
                        "table1", "ACTIVE",
                        "table2", "UPDATING",
                        "table3", "DROPPING"
                ),
                Map.of(
                        "table1", "ACTIVE",
                        "table2", "ACTIVE",
                        "table3", "DROPPING"
                ),
                Map.of(
                        "table1", "ACTIVE",
                        "table2", "ACTIVE"
                )
        ));

        assertDoesNotThrow(() -> awaitService.await(List.of(
                new TableOperation("table1", TableOperationType.CREATE),
                new TableOperation("table2", TableOperationType.UPDATE),
                new TableOperation("table3", TableOperationType.DROP)
        )));
    }
}
