package org.qubership.cloud.quarkus.dbaas.opensearch.client.service.impl;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.CommonTestMethods;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbaaSOpensearchLogicalDbProviderTest {
    DbaasClient dbaaSClient = mock(DbaasClient.class);

    @BeforeEach
    void prepareMocks() {
    }

    @Test
    public void testGetConnectionFromProvider() {
        OpensearchIndexConnection connection = CommonTestMethods.prepareOpensearchConnection();
        OpensearchIndex index = new OpensearchIndex();
        index.setConnectionProperties(connection);
        String namespace = "test_space";
        index.setNamespace(namespace);
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put(SCOPE, SERVICE);
        classifier.put("namespace", namespace);
        index.setClassifier(classifier);
        DatabaseConfig config = DatabaseConfig.builder().userRole("admin").build();
        when(dbaaSClient.getOrCreateDatabase(OpensearchDBType.INSTANCE, namespace, classifier, config)).thenReturn(index);

        DbaaSOpensearchLogicalDbProvider provider = new DbaaSOpensearchLogicalDbProvider(dbaaSClient);
        OpensearchIndex providedIndex = provider.provide(classifier, config, namespace);
        assertEquals(index, providedIndex);

    }

    @Test
    public void testProviderOrder() {
        DbaaSOpensearchLogicalDbProvider provider = new DbaaSOpensearchLogicalDbProvider(dbaaSClient);
        assertEquals(Integer.MAX_VALUE, provider.order());
    }

}