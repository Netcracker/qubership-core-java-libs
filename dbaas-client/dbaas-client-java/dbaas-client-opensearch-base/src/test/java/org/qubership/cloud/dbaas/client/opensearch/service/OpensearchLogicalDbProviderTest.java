package org.qubership.cloud.dbaas.client.opensearch.service;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.qubership.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.qubership.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProviderImpl.*;

public class OpensearchLogicalDbProviderTest {

    @Test
    public void checkOpensearchLogicalDbProvider() {
        OpensearchLogicalDbProviderImpl logicalDbProvider = new OpensearchLogicalDbProviderImpl();

        OpensearchIndex provide = logicalDbProvider.provide(Collections.emptySortedMap(), DatabaseConfig.builder().build(), "test-ns");
        OpensearchIndexConnection connectionProperties = provide.getConnectionProperties();

        Assertions.assertNotNull(connectionProperties);
        Assertions.assertEquals("my-index", provide.getName());
        Assertions.assertEquals(URL, connectionProperties.getUrl());
        Assertions.assertEquals(USERNAME, connectionProperties.getUsername());
        Assertions.assertEquals(HOST, connectionProperties.getHost());
        Assertions.assertEquals(PASSWORD, connectionProperties.getPassword());
        Assertions.assertEquals(RESOURCE_PREFIX, connectionProperties.getResourcePrefix());
        Assertions.assertEquals(PORT, connectionProperties.getPort());
        Assertions.assertEquals(ADMIN_ROLE, connectionProperties.getRole());
    }

    @Test
    public void checkOpensearchSupportedDatabaseType() {
        OpensearchLogicalDbProviderImpl logicalDbProvider = new OpensearchLogicalDbProviderImpl();
        Assertions.assertEquals(OpensearchIndex.class, logicalDbProvider.getSupportedDatabaseType());

    }
}