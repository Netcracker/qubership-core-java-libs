package com.netcracker.cloud.dbaas.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.entity.*;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.entity.database.type.PostgresDBType;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasException;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.matchers.RequestUrlMatcher;
import okhttp3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.*;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.MICROSERVICE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DbaaSClientOkHttpImplTest {

    private static final ObjectMapper JACK = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String API_V3_RELEASE_URL = "http://test-ms:8080/api/v3/dbaas/";

    private static final String API_VERSION_URL = "http://test-ms:8080/api-version";
    private static final String DBAAS_POSTGRESQL_URL = "http://test-ms:8080/api/v3/dbaas/postgresql/physical_databases";
    private static final String DATABASES = "/databases";
    private final String TEST_ADAPTER_ID = "test-adapter-id";
    private final String TEST_NAMESPACE = "test-namespace";
    private final String TEST_DB_PREFIX = "test-db-prefix";
    private final String TEST_DB_NAME = "test-db-name";
    private final String TEST_USER = "test-user";
    private final String TEST_PASSWORD = "test-password";
    private final String TEST_PHYS_DB_ID = "test-phys-db-id";

    //    @Mock
    private static OkHttpClient okHttpClient;

    private static DbaaSClientOkHttpImpl dbaaSClientOkHttp;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void init() throws IOException {
        okHttpClient = mockOkHttpClient();
        dbaaSClientOkHttp = new DbaaSClientOkHttpImpl("http://test-ms:8080", okHttpClient);
    }

    private static OkHttpClient mockOkHttpClient() throws IOException {
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        Request req = new Request.Builder()
                .url(API_VERSION_URL)
                .get().build();
        Call call = mock(Call.class);
        doReturn(call).when(okHttpClient).newCall(argThat(new RequestUrlMatcher(req)));
        return okHttpClient;
    }

    @BeforeEach
    public void setUp() throws IOException {
        Mockito.reset(okHttpClient);
    }

    @Test
    public void testGetDatabase() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PostgresDatabase postgresDatabaseSample = getPostgresDatabaseSample();
        final DatabaseType<?, ?> dbType = new DatabaseType<>("postgresql", PostgresDatabase.class);
        final Map<String, Object> classifier = new HashMap<>(2){{
            put(MICROSERVICE_NAME, "test-ms");
            put(SCOPE, SERVICE);
            put(NAMESPACE, TEST_NAMESPACE);
        }};
        final String userRole = "admin";
        final String url = "http://test-ms:8080" + DBAAS_BASE_URL + "/" + TEST_NAMESPACE + DATABASES + GET_BY_CLASSIFIER + "/" + dbType.getName();
        final Request expectedReq = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JACK.writeValueAsBytes(new GetDatabaseByClassifierRequest(classifier, userRole)), MediaType.parse("application/json"))).build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(postgresDatabaseSample)));
        AbstractDatabase<?> actualDatabase = dbaaSClientOkHttp.getDatabase(dbType, TEST_NAMESPACE, userRole, classifier);
        assertEquals(postgresDatabaseSample.getName(), actualDatabase.getName());

        //Test 404
        mockApiVersion();
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(404);
        actualDatabase = dbaaSClientOkHttp.getDatabase(dbType, TEST_NAMESPACE, userRole, classifier);
        assertNull(actualDatabase);

        //Test response is not successful and not 404
        mockResponseSuccess(false);
        mockApiVersion();
        assertThrows(DbaasException.class, () -> dbaaSClientOkHttp.getDatabase(dbType, TEST_NAMESPACE, userRole, classifier));
    }

    @Test
    public void testGetPhysicalDatabases() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PhysicalDatabases physicalDatabases = getPhysicalDatabasesSample();
        final Request expectedReq = new Request.Builder()
                .url(DBAAS_POSTGRESQL_URL)
                .get().build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(physicalDatabases)));

        final PhysicalDatabases actualPhysicalDatabases = dbaaSClientOkHttp.getPhysicalDatabases("postgresql");
        assertEquals(physicalDatabases.getIdentified().get("pg-physDb").getAdapterId(), actualPhysicalDatabases.getIdentified().get("pg-physDb").getAdapterId());
    }

    @Test
    public void testGetPhysicalDatabasesNotSuccessful() throws IOException {
        mockResponseSuccess(false);
        mockApiVersion();
        assertThrows(DbaasException.class,
                () -> dbaaSClientOkHttp.getPhysicalDatabases("postgresql"));
    }

    @Test
    public void testCreateDatabase() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PostgresDatabase postgresDatabase = getPostgresDatabaseSample();
        final Map<String, Object> classifier = TestUtil.buildServiceClassifier("test-namespace", "test_name");
        DatabaseConfig parameters = getDbCreateParameters();
        DatabaseCreateRequest body = new DatabaseCreateRequest(classifier, PostgresDBType.INSTANCE.getName(), parameters);
        final Request expectedReq = new Request.Builder()
                .url("http://test-ms:8080/api/v3/dbaas/" + TEST_NAMESPACE + "/databases")
                .put(RequestBody.create(MediaType.parse("application/json"), JACK.writeValueAsBytes(body))).build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(postgresDatabase)));
        mockApiVersion();
        final PostgresDatabase actualDatabase = dbaaSClientOkHttp.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier,
                DatabaseConfig.builder()
                        .physicalDatabaseId(TEST_PHYS_DB_ID)
                        .backupDisabled(true)
                        .dbNamePrefix(TEST_DB_PREFIX)
                        .build()
        );

        assertEquals(postgresDatabase.getNamespace(), actualDatabase.getNamespace());
        assertEquals(postgresDatabase.getName(), actualDatabase.getName());
    }

    @Test
    public void testWillDoRetryIfAggregatorReturnedAcceptedAndUnavailable() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PostgresDatabase postgresDatabase = getPostgresDatabaseSample();
        final Map<String, Object> classifier = TestUtil.buildServiceClassifier("test-namespace", "test_name");
        DatabaseConfig parameters = getDbCreateParameters();
        DatabaseCreateRequest body = new DatabaseCreateRequest(classifier, PostgresDBType.INSTANCE.getName(), parameters);
        final Request expectedReq = new Request.Builder()
                .url("http://test-ms:8080/api/v3/dbaas/" + TEST_NAMESPACE + "/databases")
                .put(RequestBody.create(MediaType.parse("application/json"), JACK.writeValueAsBytes(body))).build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.code()).thenReturn(202).thenReturn(500).thenReturn(201);
        when(response.isSuccessful()).thenReturn(false).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(postgresDatabase)));
        mockApiVersion();
        final PostgresDatabase actualDatabase = dbaaSClientOkHttp.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier,
                parameters);

        assertEquals(postgresDatabase.getNamespace(), actualDatabase.getNamespace());
        assertEquals(postgresDatabase.getName(), actualDatabase.getName());
        // assert there were two retries and one successful call
        verify(call, times(3)).execute();
    }

    @Test
    public void testReachedMaxNumberOfRetries() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final Map<String, Object> classifier = TestUtil.buildServiceClassifier("test-namespace", "test_name");
        DatabaseConfig parameters = getDbCreateParameters();
        DatabaseCreateRequest body = new DatabaseCreateRequest(classifier, PostgresDBType.INSTANCE.getName(), parameters);
        final Request expectedReq = new Request.Builder()
                .url(API_V3_RELEASE_URL + TEST_NAMESPACE + DATABASES)
                .put(RequestBody.create(MediaType.parse("application/json"), JACK.writeValueAsBytes(body))).build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.code()).thenReturn(202);
        mockApiVersion();
        assertThrows(DbaasException.class,
                () -> dbaaSClientOkHttp.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier,
                        parameters));
    }

    @Test
    public void testWillDoRetryIfAggregatorReturnedCodeAccepted() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PostgresDatabase postgresDatabase = getPostgresDatabaseSample();
        final Map<String, Object> classifier = TestUtil.buildServiceClassifier("test-namespace", "test_name");

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.code()).thenReturn(202).thenReturn(201);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(postgresDatabase)));
        mockApiVersion();

        final PostgresDatabase actualDatabase = dbaaSClientOkHttp.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, classifier,
                DatabaseConfig.builder()
                        .physicalDatabaseId(TEST_PHYS_DB_ID)
                        .backupDisabled(true)
                        .dbNamePrefix(TEST_DB_PREFIX)
                        .build());

        assertEquals(postgresDatabase.getNamespace(), actualDatabase.getNamespace());
        assertEquals(postgresDatabase.getName(), actualDatabase.getName());
        // assert there was one fallback to v1, one retry because dbaas returned 202, and one success
        verify(call, times(2)).execute();
    }

    @Test
    public void testCreateDatabaseNotSuccessful() throws IOException {
        mockResponseSuccess(false);
        mockApiVersion();
        assertThrows(DbaasException.class,
                () -> {
                    dbaaSClientOkHttp.getOrCreateDatabase(PostgresDBType.INSTANCE, TEST_NAMESPACE, TestUtil.buildServiceClassifier("test-namespace", "test_name"),
                            DatabaseConfig.builder()
                                    .physicalDatabaseId(TEST_PHYS_DB_ID)
                                    .backupDisabled(true)
                                    .dbNamePrefix(TEST_DB_PREFIX)
                                    .build());
                });
    }


    @Test
    public void testGetConnectionIsSuccessful() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);
        final PostgresDatabase postgresDatabase = getPostgresDatabaseSample();
        final Map<String, Object> classifier = new HashMap<String, Object>() {
            {
                put("scope", "service");
                put("microserviceName", "test_name");
                put("namespace", "test_namespace");
            }
        };
        GetDatabaseByClassifierRequest getDatabaseByClassifierRequest = new GetDatabaseByClassifierRequest(classifier, null);
        final Request expectedReq = new Request.Builder()
                .url(API_V3_RELEASE_URL + TEST_NAMESPACE + "/databases/get-by-classifier/postgresql")
                .post(RequestBody.create(MediaType.parse("application/json"), JACK.writeValueAsBytes(getDatabaseByClassifierRequest))).build();

        when(okHttpClient.newCall(argThat(new RequestUrlMatcher(expectedReq)))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(postgresDatabase)));

        final PostgresDBConnection actualConnection = dbaaSClientOkHttp.getConnection(PostgresDBType.INSTANCE, TEST_NAMESPACE, null, classifier);

        final PostgresDBConnection connection = getPostgresDatabaseSample().getConnectionProperties();
        assertEquals(connection.getUsername(), actualConnection.getUsername());
        assertEquals(connection.getPassword(), actualConnection.getPassword());
    }

    @Test
    public void testGetConnectionReturn404() throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final Map<String, Object> classifier = new HashMap<String, Object>() {
            {
                put("scope", "service");
                put("microserviceName", "test_name");
                put("namespace", "test_namespace");
            }
        };

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.code()).thenReturn(404);
        when(response.isSuccessful()).thenReturn(false);

        mockApiVersion();
        assertNull(dbaaSClientOkHttp.getConnection(PostgresDBType.INSTANCE, TEST_NAMESPACE, null, classifier));
    }

    @Test
    public void testCheckDbaaSApiVersion() throws IOException {
        String address = "http://test-ms:8080";
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);

        // Prepare the mock response JSON
        String mockApiVersionResponseJson = "{"
                + "\"major\": 3,"
                + "\"minor\": 14,"
                + "\"supportedMajors\": [3],"
                + "\"specs\":["
                + "    {\"specRootUrl\":\"/api\",\"major\":3,\"minor\":14,\"supportedMajors\":[3]},"
                + "    {\"specRootUrl\":\"/api/declarations\",\"major\":1,\"minor\":0,\"supportedMajors\":[1]},"
                + "    {\"specRootUrl\":\"/api/composite\",\"major\":1,\"minor\":0,\"supportedMajors\":[1]}"
                + "]"
                + "}";

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(mockApiVersionResponseJson.getBytes());

        dbaaSClientOkHttp.checkDbaaSApiVersion(address);
        verify(call, times(1)).execute();

        ObjectMapper objectMapper = new ObjectMapper();
        ApiVersionInfo apiVersionInfo = objectMapper.readValue(mockApiVersionResponseJson, ApiVersionInfo.class);

        // Assert that the major, minor, and supportedMajors fields are correctly processed
        assertEquals(3, apiVersionInfo.getMajor().intValue());
        assertEquals(14, apiVersionInfo.getMinor().intValue());
        assertNotNull(apiVersionInfo.getSupportedMajors());
        assertTrue(apiVersionInfo.getSupportedMajors().contains(3));
    }

    @Test
    public void testCheckDbaaSApiVersionWithoutTopLevelFields() throws IOException {
        String address = "http://test-ms:8080";
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);
        final ResponseBody responseBody = mock(ResponseBody.class);

        String mockApiVersionResponseJson = "{"
                + "\"specs\":["
                + "    {\"specRootUrl\":\"/api\",\"major\":3,\"minor\":9,\"supportedMajors\":[3]},"
                + "    {\"specRootUrl\":\"/api/declarations\",\"major\":1,\"minor\":0,\"supportedMajors\":[1]},"
                + "    {\"specRootUrl\":\"/api/composite\",\"major\":1,\"minor\":0,\"supportedMajors\":[1]}"
                + "]"
                + "}";

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(mockApiVersionResponseJson.getBytes());

        dbaaSClientOkHttp.checkDbaaSApiVersion(address);
        verify(call, times(1)).execute();

        ObjectMapper objectMapper = new ObjectMapper();
        ApiVersionInfo apiVersionInfo = objectMapper.readValue(mockApiVersionResponseJson, ApiVersionInfo.class);

        // Assert that the major and minor fields are null (since they are not present in the JSON)
        assertNull(apiVersionInfo.getMajor());
        assertNull(apiVersionInfo.getMinor());

        // Assert that the supportedMajors is correctly recieved from the first spec entry
        assertNotNull(apiVersionInfo.getSupportedMajors());
        assertTrue(apiVersionInfo.getSupportedMajors().contains(3));
    }

    private void mockResponseSuccess(boolean successful) throws IOException {
        final Call call = mock(Call.class);
        final Response response = mock(Response.class);

        doReturn(call).when(okHttpClient).newCall(any());
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(successful);
    }

    private PhysicalDatabases getPhysicalDatabasesSample() {
        final PhysicalDatabases physicalDatabases = new PhysicalDatabases();
        final PhysicalDatabaseDescription physicalDatabaseDescription = new PhysicalDatabaseDescription();
        physicalDatabaseDescription.setAdapterId(TEST_ADAPTER_ID);
        physicalDatabases.setIdentified(Collections.singletonMap("pg-physDb", physicalDatabaseDescription));
        return physicalDatabases;
    }

    private PostgresDatabase getPostgresDatabaseSample() {
        final PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setNamespace(TEST_NAMESPACE);
        postgresDatabase.setName(TEST_DB_NAME);
        PostgresDBConnection connection = new PostgresDBConnection();
        connection.setUsername(TEST_USER);
        connection.setPassword(TEST_PASSWORD);
        postgresDatabase.setConnectionProperties(connection);
        return postgresDatabase;
    }

    private DatabaseConfig getDbCreateParameters() {
        return DatabaseConfig.builder()
                .physicalDatabaseId(TEST_PHYS_DB_ID)
                .backupDisabled(true)
                .dbNamePrefix(TEST_DB_PREFIX)
                .build();
    }

    private void mockApiVersion() throws IOException {
        ApiVersionInfo apiVersionInfo = new ApiVersionInfo();
        apiVersionInfo.setSupportedMajors(Arrays.asList(3));
        Request apiVersionReq = new Request.Builder()
                .url("http://test-ms:8080/api-version")
                .get().build();
        final Call apiVersionCall = mock(Call.class);
        final Response apiVersionResponse = mock(Response.class);
        final ResponseBody apiVersionResponseBody = mock(ResponseBody.class);
        when(apiVersionCall.execute()).thenReturn(apiVersionResponse);
        when(apiVersionResponse.isSuccessful()).thenReturn(true);
        when(apiVersionResponse.body()).thenReturn(apiVersionResponseBody);
        when(apiVersionResponseBody.bytes()).thenReturn(objectMapper.writeValueAsBytes(apiVersionInfo));
        doReturn(apiVersionCall).when(okHttpClient).newCall(argThat(new RequestUrlMatcher(apiVersionReq)));
    }
}
