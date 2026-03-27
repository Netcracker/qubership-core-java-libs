package com.netcracker.cloud.quarkus.dbaas.opensearch.client.service.impl;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import com.netcracker.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchConfigurationProperty;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.SinglePrefix;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpensearchDbaaSApiClientImplTest {

    DbaasDbClassifier mockClassifier;
    DbaaSOpensearchCreationConfig creationConfig;
    SinglePrefix singleTenantPrefixConfig;
    SinglePrefix servicePrefixConfig;
    DbaaSOpensearchConfigurationProperty configProp;
    OpensearchLogicalDbProvider logicalDbProvider;
    Instance<OpensearchLogicalDbProvider> dbProvidersInstance;
    DbaasClient dbaasClient;
    OpenSearchClient openSearchClient;

    private static final String TENANT_ID_VALUE = "tenant-123";
    private static final String SERVICE_PREFIX = "service-prefix";
    private static final String SINGLE_TENANT_PREFIX_TEMPLATE = "tenant-prefix-{tenantId}";
    private static final String SINGLE_TENANT_PREFIX_RESOLVED = "tenant-prefix-tenant-123";
    private static final String TEST_USERNAME = "username";
    private static final String TEST_NAMESPACE = "test-namespace";

    @BeforeEach
    void prepareMocks() {
        openSearchClient = mock(OpenSearchClient.class);

        singleTenantPrefixConfig = mock(SinglePrefix.class);
        servicePrefixConfig = mock(SinglePrefix.class);

        creationConfig = mock(DbaaSOpensearchCreationConfig.class);
        when(creationConfig.getOpensearchConfiguration(any())).thenReturn(null);
        when(creationConfig.runtimeUserRole()).thenReturn(Optional.empty());

        configProp = mock(DbaaSOpensearchConfigurationProperty.class);
        when(configProp.maxConnTotal()).thenReturn(Optional.empty());
        when(configProp.maxConnPerRoute()).thenReturn(Optional.empty());

        dbaasClient = mock(DbaasClient.class);

        logicalDbProvider = mock(OpensearchLogicalDbProvider.class);
        when(logicalDbProvider.order()).thenReturn(0);

        dbProvidersInstance = mock(Instance.class);
        when(dbProvidersInstance.stream()).thenReturn(Stream.of(logicalDbProvider));

        mockClassifier = mock(DbaasDbClassifier.class);
    }

    private OpensearchIndex buildOpensearchIndex(String prefix, SortedMap<String, Object> classifierMap) {
        OpensearchIndexConnection connection = new OpensearchIndexConnection();
        connection.setResourcePrefix(prefix);
        connection.setUsername(TEST_USERNAME);
        connection.setPassword("test-password");
        connection.setHost("localhost");
        connection.setPort(9200);
        connection.setUrl("http://localhost:9200");
        connection.setTls(false);
        connection.setOpenSearchClient(openSearchClient);

        OpensearchIndex opensearchIndex = new OpensearchIndex();
        opensearchIndex.setConnectionProperties(connection);
        opensearchIndex.setClassifier(classifierMap);
        return opensearchIndex;
    }

    private SortedMap<String, Object> buildTenantClassifierMap() {
        SortedMap<String, Object> classifierMap = new TreeMap<>();
        classifierMap.put(SCOPE, TENANT);
        classifierMap.put(TENANT_ID, TENANT_ID_VALUE);
        return classifierMap;
    }

    @Test
    void testGetDatabasePrefixUsesSingleTenantPrefixWhenPrefixPresent() {
        SortedMap<String, Object> classifierMap = buildTenantClassifierMap();
        when(mockClassifier.asMap()).thenReturn(classifierMap);

        when(singleTenantPrefixConfig.prefix()).thenReturn(Optional.of(SINGLE_TENANT_PREFIX_TEMPLATE));
        when(creationConfig.singleTenantPrefixConfig()).thenReturn(singleTenantPrefixConfig);

        when(logicalDbProvider.provide(any(), any(), any()))
                .thenReturn(buildOpensearchIndex(SINGLE_TENANT_PREFIX_RESOLVED, classifierMap));

        OpensearchDbaaSApiClientImpl apiClient = new OpensearchDbaaSApiClientImpl(
                TEST_NAMESPACE, dbProvidersInstance, creationConfig, dbaasClient, configProp);

        OpensearchIndexConnection result = apiClient.getOrCreateOpensearchIndex(mockClassifier);

        assertEquals(SINGLE_TENANT_PREFIX_RESOLVED, result.getResourcePrefix());
        verify(logicalDbProvider).provide(any(),
                argThat(config -> SINGLE_TENANT_PREFIX_RESOLVED.equals(config.getDbNamePrefix())),
                eq(TEST_NAMESPACE));
    }

    @Test
    void testGetDatabasePrefixFallsBackToServicePrefixWhenSingleTenantPrefixAbsent() {
        SortedMap<String, Object> classifierMap = buildTenantClassifierMap();
        when(mockClassifier.asMap()).thenReturn(classifierMap);

        // singleTenantPrefixConfig is non-null but prefix() is empty —
        // the old duplicate != null check would have entered this branch anyway,
        // calling .get() on an empty Optional and failing
        when(singleTenantPrefixConfig.prefix()).thenReturn(Optional.empty());
        when(creationConfig.singleTenantPrefixConfig()).thenReturn(singleTenantPrefixConfig);
        when(servicePrefixConfig.prefix()).thenReturn(Optional.of(SERVICE_PREFIX));
        when(creationConfig.servicePrefixConfig()).thenReturn(servicePrefixConfig);

        when(logicalDbProvider.provide(any(), any(), any()))
                .thenReturn(buildOpensearchIndex(SERVICE_PREFIX, classifierMap));

        OpensearchDbaaSApiClientImpl apiClient = new OpensearchDbaaSApiClientImpl(
                TEST_NAMESPACE, dbProvidersInstance, creationConfig, dbaasClient, configProp);

        OpensearchIndexConnection result = apiClient.getOrCreateOpensearchIndex(mockClassifier);

        assertEquals(SERVICE_PREFIX, result.getResourcePrefix());
        verify(logicalDbProvider).provide(any(),
                argThat(config -> SERVICE_PREFIX.equals(config.getDbNamePrefix())),
                eq(TEST_NAMESPACE));
    }
}
