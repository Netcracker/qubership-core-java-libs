package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.impl.CassandraClientCreationImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.it.TestPostConnectProcessor.POST_PROCESSED_OBJECT_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@QuarkusTest
@TestProfile(CassandraResourceProfile.class)
class CassandraClientTest {
    @Inject
    CassandraClientCreationImpl cassandraClientCreation;

    @Test
    void mustExecuteOnlyCassandraDatabasePostProcessors() {
        assertDoesNotThrow(() -> getSession(), "FakeDbPostProcessor shouldn't be executed when creating a session");
    }

    @Test
    void mustConnectToCassandraDb() {
        CqlSession session = getSession();
        ResultSet resultSet = session.execute("select * from testObjects where id='object2'");
        Iterator<Row> iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        Row row = iterator.next();
        assertEquals("test object 2", row.getString("name"));
        assertFalse(iterator.hasNext());
    }

    @Test
    void configPropertiesAreProcessed() {
        CqlSession session = getSession();
        // 1 minute is defined in application.properties file
        assertEquals(Duration.ofMinutes(1), session.getContext().getConfig().getDefaultProfile().getDuration(DefaultDriverOption.REQUEST_TIMEOUT));
        // value is changing during handling customizers. 1 -> 5 -> 50 (see CassandraClientTestConfiguration)
        assertEquals(Duration.ofMinutes(50), session.getContext().getConfig().getDefaultProfile().getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT));
    }

    @Test
    void mustExecuteOnlyCassandraPostConnectProcessors() {
        CqlSession session = getSession();
        ResultSet resultSet = session.execute("select * from testObjects where id=?", POST_PROCESSED_OBJECT_ID);
        assertTrue(resultSet.iterator().hasNext());
    }

    CqlSession getSession() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);

        return cassandraClientCreation.getOrCreateCassandraDatabase(classifier).getConnectionProperties().getSession();
    }
}
