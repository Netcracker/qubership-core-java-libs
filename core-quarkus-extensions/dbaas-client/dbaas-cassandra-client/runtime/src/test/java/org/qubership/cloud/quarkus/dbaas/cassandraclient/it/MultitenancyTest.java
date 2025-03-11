package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import static org.qubership.cloud.quarkus.dbaas.cassandraclient.config.CassandraClientConfiguration.SERVICE_CASSANDRA_CLIENT;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.config.CassandraClientConfiguration.TENANT_CASSANDRA_CLIENT;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.it.TestContainersCassandraLogicalDbProvider.SERVICE_KEYSPACE;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.it.TestContainersCassandraLogicalDbProvider.TENANT_KEYSPACE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(CassandraResourceProfile.class)
class MultitenancyTest {

    static String FIRST_TENANT = "a";
    static String SECOND_TENANT = "b";
    @Inject
    @Named(SERVICE_CASSANDRA_CLIENT)
    CqlSession serviceClient;

    @Inject
    @Named(TENANT_CASSANDRA_CLIENT)
    CqlSession tenantClient;

    @Test
    void serviceClientMustConnectToServiceDb() {
        assertEquals(SERVICE_KEYSPACE, serviceClient.getKeyspace().get().asInternal());
    }

    @Test
    void tenantClientMustConnectToCurrentTenantDb() {
        TenantContext.set(FIRST_TENANT);
        assertEquals(String.format(TENANT_KEYSPACE_FORMAT, FIRST_TENANT), tenantClient.getKeyspace().get().asInternal());
        TenantContext.set(SECOND_TENANT);
        assertEquals(String.format(TENANT_KEYSPACE_FORMAT, SECOND_TENANT), tenantClient.getKeyspace().get().asInternal());
    }
}
