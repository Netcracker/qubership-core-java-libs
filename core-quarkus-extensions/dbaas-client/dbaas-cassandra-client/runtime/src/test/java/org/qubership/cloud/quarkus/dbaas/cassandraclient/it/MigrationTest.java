package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;

@QuarkusTest
@TestProfile(CassandraResourceProfile.class)
class MigrationTest {

    @Inject
    CassandraClientCreation cassandraClientCreation;

    @Test
    void checkMigrationPerformed() {
        CqlSession cqlSession = cassandraClientCreation.getOrCreateCassandraDatabase(getServiceClassifier()).getConnectionProperties().getSession();
        ResultSet resultSet = cqlSession.execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name='service_db'");
        List<String> tables = resultSet.all().stream().map(row -> row.getString(0)).toList();
        Assertions.assertTrue(tables.containsAll(List.of("sample_migration_table_1", "sample_migration_table_2")));
    }

    private DbaasDbClassifier getServiceClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(params);
    }
}
