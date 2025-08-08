package com.netcracker.cloud.dbaas.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.dbaas.client.entity.ApiVersionInfo;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseDefinitionHandler;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.matchers.RequestUrlMatcher;
import okhttp3.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.qubership.cloud.dbaas.client.DbaasConst.MICROSERVICE_NAME;
import static org.qubership.cloud.dbaas.client.DbaasConst.NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresLightTest {

    private DatabasePool pool;
    private static final ObjectMapper JACK = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    public void init() throws IOException {
        OkHttpClient client = mockOkHttpClient();
        DbaaSClientOkHttpImpl dbaasClient = new DbaaSClientOkHttpImpl("http://dbaas-agent:8080", client);
        pool = new DatabasePool(dbaasClient,
                // pass address, where DbaaS API available for you
                // also you need to configure OkHttpClient
                // providing authorization if necessary

                null,
                // past here your way of configuring microservice name or skip to
                // use environment  DatabasePool.MICROSERVICE_NAME_ENV instead

                null,
                // past here your way of configuring cloud namespace or skip to
                // use environment  DatabasePool.CLOUD_NAMESPACE_ENV instead

                Collections.singletonList(new PostConnectProcessor<PostgresDatabase>() {
                    @Override
                    public void process(PostgresDatabase database) {
                        database.getConnectionProperties().setDataSource(Mockito.mock(DataSource.class));
                    }

                    @Override
                    public Class<PostgresDatabase> getSupportedDatabaseType() {
                        return PostgresDatabase.class;
                    }
                }),
                // past here your way of configuring datasource

                new DatabaseDefinitionHandler(Optional.empty(), Optional.empty(), dbaasClient));
        Mockito.doAnswer((Answer<Call>) invocationOnMock -> mockedDatabaseCreate())
                .when(client).newCall(any());
        // past here your way of ordering datasources
    }

    @Test
    public void createDatabaseTest() {
        PostgresDatabase database1 = pool
                .getOrCreateDatabase(PostgresDBType.INSTANCE,
                        new ServiceDbaaSClassifierBuilder(null)
                                .withProperty(NAMESPACE, "test-namespace")
                                .withProperty(MICROSERVICE_NAME, "test")
                                .build());

        PostgresDatabase database2 = pool
                .getOrCreateDatabase(PostgresDBType.INSTANCE,
                        new ServiceDbaaSClassifierBuilder(null)
                                .withProperty(NAMESPACE, "test-namespace")
                                .withProperty(MICROSERVICE_NAME, "test")
                                .build());
        Assertions.assertSame(database1, database2);

        PostgresDatabase database3 = pool
                .getOrCreateDatabase(PostgresDBType.INSTANCE,
                        new ServiceDbaaSClassifierBuilder(new DbaaSChainClassifierBuilder(null) {
                            @Override
                            public DbaasDbClassifier build() {
                                this.getWrapped()
                                        .withProperty("something-new", "and-the-value")
                                        .withProperty(NAMESPACE, "test-namespace")
                                        .withProperty(MICROSERVICE_NAME, "test");
                                return super.build();
                            }
                        }).build());
        Assertions.assertNotSame(database3, database1);

    }

    private static OkHttpClient mockOkHttpClient() throws IOException {
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        Request req = new Request.Builder()
                .url("http://dbaas-agent:8080/api-version")
                .get().build();
        Response response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        ResponseBody responseBody = mock(ResponseBody.class);
        ApiVersionInfo apiVersionInfo = new ApiVersionInfo(3, 1,List.of(3),List.of());
        when(responseBody.bytes()).thenReturn(JACK.writeValueAsBytes(apiVersionInfo));
        when(response.body()).thenReturn(responseBody);
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(req)))).thenReturn(call);
        return okHttpClient;
    }

    private static Call mockedDatabaseCreate() throws IOException {
        Call createDatabaseCall = Mockito.mock(Call.class);
        Response resp = Mockito.mock(Response.class);
        ResponseBody body = Mockito.mock(ResponseBody.class);
        String answer = "{\n" +
                "  \"name\": \"dbnamegenerated\",\n" +
                "  \"connectionProperties\": {\n" +
                "     \"host\": \"some-host\",\n" +
                "     \"port\": 5432,\n" +
                "     \"username\": \"user-generated\",\n" +
                "     \"password\": \"password-generated\",\n" +
                "     \"url\": \"postgresjdbs://some-host:5432/dbnamegenerated\"\n" +
                "  }\n" +
                "}";
        when(body.byteStream()).thenReturn(new ByteArrayInputStream(answer.getBytes()));
        when(body.string()).thenReturn(answer);
        when(body.bytes()).thenReturn(answer.getBytes());
        when(resp.body()).thenReturn(body);
        when(resp.code()).thenReturn(201);
        when(resp.isSuccessful()).thenReturn(true);
        when(createDatabaseCall.execute()).thenReturn(resp);
        return createDatabaseCall;
    }
}
