package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestContainersCassandraLogicalDbProvider extends CassandraLogicalDbProvider {
    public static final String SERVICE_KEYSPACE = "service_db";
    public static final String TENANT_KEYSPACE_FORMAT = "tenant_db_%s";

    @ConfigProperty(name = "quarkus.cassandra.contact-points")
    String cassandraContactPoint;

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    @Override
    public @Nullable CassandraConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        String[] connectPoints = cassandraContactPoint.split(":");
        Object isServiceDbKey = classifier.get(SCOPE);
        String keyspace;
        if (isServiceDbKey != null && isServiceDbKey.equals(SERVICE)) {
            keyspace = SERVICE_KEYSPACE;
        } else {
            keyspace = String.format(TENANT_KEYSPACE_FORMAT, TenantContext.get());
        }
        CassandraConnectionProperty cassandraDBConnection = new CassandraConnectionProperty(
                null,
                null,
                null,
                List.of(connectPoints[0]),
                keyspace,
                Integer.parseInt(connectPoints[1]),
                databaseConfig.getUserRole(),
                false
        );
        return cassandraDBConnection;
    }

    @Override
    public void provideDatabaseInfo(CassandraDatabase database) {
        String scope = String.valueOf(database.getClassifier().get(SCOPE));
        if (SERVICE.equals(scope)) {
            database.setName(SERVICE_KEYSPACE);
        } else {
            database.setName(String.format(TENANT_KEYSPACE_FORMAT, database.getClassifier().get(TENANT_ID)));
        }
    }
}
