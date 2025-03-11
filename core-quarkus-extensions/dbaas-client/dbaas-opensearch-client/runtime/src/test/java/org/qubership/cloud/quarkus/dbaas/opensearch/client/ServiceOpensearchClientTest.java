package org.qubership.cloud.quarkus.dbaas.opensearch.client;

import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.common.classifier.DbaaSClassifierFactory;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.ExistsRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceOpensearchClientTest {

    private static DbaasOpensearchClientImpl dbaaSOpensearchClient;
    private static final OpensearchDbaaSApiClient opensearchApiClient = mock(OpensearchDbaaSApiClient.class);
    private static OpenSearchClient client = mock(OpenSearchClient.class);
    private static OpensearchIndexConnection opensearchIdxConnection;
    private static DbaaSClassifierFactory factory = mock(DbaaSClassifierFactory.class);

    @BeforeEach
    void prepare() throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        when(factory.newServiceClassifierBuilder(params)).thenCallRealMethod();
        dbaaSOpensearchClient = new DbaasOpensearchClientImpl(factory.newServiceClassifierBuilder(params), opensearchApiClient, "_");
        opensearchIdxConnection = CommonTestMethods.prepareOpensearchConnection();
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        opensearchIdxConnection.setOpenSearchClient(client);
    }

    @Test
    void mustReturnSameServiceOpensearchClient() {
        when(opensearchApiClient.getOpensearchIndex(any())).thenReturn(opensearchIdxConnection);
        when(opensearchApiClient.getOrCreateOpensearchIndex(any())).thenReturn(opensearchIdxConnection);

        OpenSearchClient firstClient = dbaaSOpensearchClient.getOrCreateIndex().getOpenSearchClient();
        assertNotNull(firstClient);

        OpenSearchClient secondClient = dbaaSOpensearchClient.getOrCreateIndex().getOpenSearchClient();
        assertEquals(firstClient, secondClient);
    }
}