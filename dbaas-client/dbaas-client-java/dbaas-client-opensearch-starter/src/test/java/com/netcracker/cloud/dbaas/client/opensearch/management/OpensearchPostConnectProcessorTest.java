package org.qubership.cloud.dbaas.client.opensearch.management;

import org.qubership.cloud.dbaas.client.exceptions.DbaasException;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClientBuilderCustomizer;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.DefaultDbaasOpensearchClientBuilderCustomizer;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OpensearchPostConnectProcessorTest {

    public final static String DB_NAME = "index-test-name";
    public final static String DB_USER = "dbaas";
    public final static String DB_PASSWORD = "dbaas";
    public final static String DB_URL = "http://test-es-idx-host:9200";
    public final static String DB_HOST = "test-es-idx-host";

    @Test
    public void checkExceptionWhenNoCredsWereProvided() {
        assertThrows(DbaasException.class,
                () -> {
                    DbaaSOpensearchConfigurationProperty props = new DbaaSOpensearchConfigurationProperty();
                    OpensearchPostConnectProcessor processor = new OpensearchPostConnectProcessor(props, Collections.singletonList(new DefaultDbaasOpensearchClientBuilderCustomizer()), null);
                    OpensearchIndex index = createOpensearchIndex();
                    processor.process(index);
                });
    }

    @Test
    public void checkCanGetCredentialsFromDbaas() {
        String username = "user";
        String password = "passwd";
        DbaaSOpensearchConfigurationProperty props = new DbaaSOpensearchConfigurationProperty();
        OpensearchPostConnectProcessor processor = new OpensearchPostConnectProcessor(props, Collections.singletonList(new DefaultDbaasOpensearchClientBuilderCustomizer()), null);
        OpensearchIndex index = createOpensearchIndex();
        index.setConnectionProperties(setCredentials(username, password, index));
        processor.process(index);
        OpenSearchClient client = index.getConnectionProperties().getOpenSearchClient();
        Assertions.assertNotNull(client);
    }

    @Test
    void checkCanProcessTLSConnection() {
        String username = "user";
        String password = "passwd";
        DbaaSOpensearchConfigurationProperty props = new DbaaSOpensearchConfigurationProperty();
        OpensearchPostConnectProcessor processor = new OpensearchPostConnectProcessor( props, Collections.singletonList(new DefaultDbaasOpensearchClientBuilderCustomizer()), null);
        OpensearchIndex index = createOpensearchIndex();
        index.getConnectionProperties().setTls(true);
        index.getConnectionProperties().setRole("test-role");
        index.setConnectionProperties(setCredentials(username, password, index));
        processor.process(index);
        OpenSearchClient client = index.getConnectionProperties().getOpenSearchClient();
        Assertions.assertNotNull(client);
        assertTrue(index.getConnectionProperties().getUrl().startsWith("https"));
    }

    @Test
    void checkCanDisableTLSConnection() {
        String username = "user";
        String password = "passwd";
        DbaaSOpensearchConfigurationProperty props = new DbaaSOpensearchConfigurationProperty();
        OpensearchPostConnectProcessor processor = new OpensearchPostConnectProcessor(props, Collections.singletonList(new DefaultDbaasOpensearchClientBuilderCustomizer()), null);
        OpensearchIndex index = createOpensearchIndex();
        index.getConnectionProperties().setTls(false);
        index.getConnectionProperties().setUrl("https://localhost:9200/");
        index.getConnectionProperties().setRole("test-role");
        index.setConnectionProperties(setCredentials(username, password, index));
        processor.process(index);
        OpenSearchClient client = index.getConnectionProperties().getOpenSearchClient();
        Assertions.assertNotNull(client);
        assertFalse(index.getConnectionProperties().getUrl().startsWith("https"));
    }


    private OpensearchIndexConnection setCredentials(String username, String password, OpensearchIndex index) {
        OpensearchIndexConnection connection = index.getConnectionProperties();
        connection.setUsername(username);
        connection.setPassword(password);
        index.setConnectionProperties(connection);
        return connection;
    }

    protected OpensearchIndex createOpensearchIndex() {
        OpensearchIndex opensearchIndex = new OpensearchIndex();
        opensearchIndex.setName(DB_NAME);
        opensearchIndex.setClassifier(new TreeMap<>());
        OpensearchIndexConnection opensearchIndexConnection = new OpensearchIndexConnection();
        opensearchIndexConnection.setHost(DB_HOST);
        opensearchIndexConnection.setUrl(DB_URL);
        opensearchIndex.setConnectionProperties(opensearchIndexConnection);
        return opensearchIndex;
    }

    @Test
    void shouldApplyCustomizerSettings() {
        String username = "user";
        String password = "passwd";
        DbaaSOpensearchConfigurationProperty mockConfigProperty = mock(DbaaSOpensearchConfigurationProperty.class);;
        DbaasOpensearchClientBuilderCustomizer mockCustomizer = mock(DbaasOpensearchClientBuilderCustomizer.class);
        DbaaSMetricsRegistrar mockMetricsRegistrar = mock(DbaaSMetricsRegistrar.class);

        OpensearchPostConnectProcessor processor = new OpensearchPostConnectProcessor(
                mockConfigProperty,
                List.of(mockCustomizer),
                mockMetricsRegistrar);

        OpensearchIndex index = createOpensearchIndex();
        index.getConnectionProperties().setTls(false);
        index.getConnectionProperties().setUrl("https://localhost:9200/");
        index.getConnectionProperties().setRole("test-role");
        index.setConnectionProperties(setCredentials(username, password, index));
        processor.process(index);

        ArgumentCaptor<PoolingAsyncClientConnectionManagerBuilder> captor = ArgumentCaptor.forClass(PoolingAsyncClientConnectionManagerBuilder.class);
        verify(mockCustomizer, times(1)).customize(captor.capture());
        PoolingAsyncClientConnectionManagerBuilder capturedBuilder = captor.getValue();
        capturedBuilder.setMaxConnPerRoute(50);
        capturedBuilder.setMaxConnTotal(100);

        verify(mockCustomizer).customize(any(ApacheHttpClient5TransportBuilder.class));
        verify(mockCustomizer).customize(any(PoolingAsyncClientConnectionManagerBuilder.class));

    }

}