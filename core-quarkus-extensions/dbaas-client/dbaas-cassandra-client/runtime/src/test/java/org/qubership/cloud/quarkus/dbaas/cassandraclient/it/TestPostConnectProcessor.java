package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.common.postprocessor.QuarkusPostConnectProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestPostConnectProcessor implements QuarkusPostConnectProcessor<CassandraDatabase> {
    public static final String POST_PROCESSED_OBJECT_ID = "post-processed-object-1";

    private static final String INSERT_POST_PROCESSED_OBJECT_QUERY = "INSERT INTO testObjects(id, name) values ('" + POST_PROCESSED_OBJECT_ID + "', 'test object from post processor')";

    @Override
    public void process(CassandraDatabase database) {
        CqlSession session = database.getConnectionProperties().getSession();
        session.getKeyspace().ifPresent(cqlIdentifier -> {
            if ("service_db".equals(cqlIdentifier.asInternal())) {
                session.execute(INSERT_POST_PROCESSED_OBJECT_QUERY);
            }
        });
    }
}
