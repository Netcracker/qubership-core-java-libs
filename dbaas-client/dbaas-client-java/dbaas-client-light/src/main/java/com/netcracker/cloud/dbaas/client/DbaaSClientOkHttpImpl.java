package com.netcracker.cloud.dbaas.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.entity.ApiVersionInfo;
import com.netcracker.cloud.dbaas.client.entity.DatabaseCreateRequest;
import com.netcracker.cloud.dbaas.client.entity.GetDatabaseByClassifierRequest;
import com.netcracker.cloud.dbaas.client.entity.PhysicalDatabases;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasException;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasUnavailableException;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.service.ClassifierChecker;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.*;

@Slf4j
public class DbaaSClientOkHttpImpl implements DbaasClient {

    private static final MediaType JSON = MediaType.parse("application/json");

    private static final String FAILED = "Failed put request to ";

    private static final String MARSHALLING_FAILED = "Marshalling failed during put request to ";
    private static final ObjectMapper JACK = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private OkHttpClient client;
    private String address;
    private final ClassifierChecker classifierChecker;

    public DbaaSClientOkHttpImpl(String address, OkHttpClient client) {
        this.address = address;
        this.client = client == null ? buildDefaultClient() : client;
        classifierChecker = new ClassifierChecker();
    }

    void checkDbaaSApiVersion(String address) {
        ApiVersionInfo apiVersionInfo;
        Request req = new Request.Builder()
                .url(address + "/api-version")
                .get().build();
        try (Response response = this.client.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Error to get current dbaas API version. " +
                        "Your installed dbaas does not support v3 version or there is some problem with connectivity.");
            }
            apiVersionInfo = JACK.readValue(response.body().bytes(), ApiVersionInfo.class);
            if (!apiVersionInfo.getSupportedMajors().contains(3)) {
                throw new RuntimeException("The current version of dbaas-client library is not compatible with dbaas-aggregator. This dbaas-client can only work with dbaas v3 " +
                        "but dbaas-aggregator supports the following api versions: " + apiVersionInfo.getSupportedMajors());
            }
        } catch (IOException exception) {
            throw new RuntimeException("Error to get current dbaas API version.", exception);
        }
    }

    public static OkHttpClient buildDefaultClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpErrorRetrier(
                        Collections.singletonList(503),
                        10,
                        2)).build();
    }

    @Override
    public PhysicalDatabases getPhysicalDatabases(String type)
            throws DbaasException, DbaasUnavailableException {
        log.info("Requesting physical databases of type {} from {}",
                type, address);
        String url = address + DBAAS_BASE_URL + "/" + type + PHYSICAL_DATABASES;
        Request req = new Request.Builder()
                .url(url)
                .get().build();
        try (Response response = client.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                checkDbaaSApiVersion(address);
                String responseBody = response.body() != null ? response.body().string() : null;
                String message = "Failed to get from " + url + " with code " + response.code() +
                        (responseBody != null ? (" and body: " + responseBody) : "");
                log.error(message);
                throw new DbaasException(message);
            }
            return JACK.readValue(response.body().byteStream(), PhysicalDatabases.class);
        } catch (JsonProcessingException e) {
            log.error(MARSHALLING_FAILED + url, e);
            throw new DbaasException(e);
        } catch (IOException ioe) {
            log.error(FAILED + url, ioe);
            throw new DbaasException(ioe);
        }
    }

    @Override
    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(final DatabaseType<T, D> type,
                                                                    final String namespace,
                                                                    final Map<String, Object> classifier,
                                                                    final DatabaseConfig databaseConfig) {
        log.info("Requesting database of type {} create from {} for namespace {} and classifier {}",
                type.getName(), address, namespace, classifier);
        classifierChecker.check(classifier);
        DatabaseCreateRequest body = new DatabaseCreateRequest(classifier, type.getName(), databaseConfig);
        Class<? extends D> databaseClass = type.getDatabaseClass();
        String url = address + DBAAS_BASE_URL + "/" + namespace + DATABASES;
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(JSON, JACK.writeValueAsBytes(body))).build();
            try (Response response = client.newCall(req).execute()) {
                // HttpStatus Accepted(202) means that database was requested but not ready yet
                int code = response.code();
                if ((code != 200) && (code != 201)) {
                    checkDbaaSApiVersion(address);
                    return doRetryRequest(url, req, onDatabaseCreate(databaseClass, body, url));
                }
                return JACK.readValue(response.body().byteStream(), databaseClass);
            }
        } catch (DbaasException exc) {
            throw exc;
        } catch (JsonProcessingException e) {
            log.error(MARSHALLING_FAILED + url, e);
            throw new DbaasException(e);
        } catch (IOException exc) {
            log.error(FAILED + url, exc);
            throw new DbaasException(exc);
        }
    }

    private <D> Function<Response, D> onDatabaseCreate(Class<? extends D> databaseClass, DatabaseCreateRequest body, String url) {
        return responseAfterRetry -> {
            ResponseBody responseBody = responseAfterRetry.body();
            if (responseBody != null) {
                try {
                    return JACK.readValue(responseBody.byteStream(), databaseClass);
                } catch (IOException e) {
                    log.error(MARSHALLING_FAILED + url, e);
                    throw new DbaasException(e);
                }
            }
            String message = "Failed to get database: "
                    + body + " . Http code: " + responseAfterRetry.code();
            log.error(message);
            throw new DbaasException(message);
        };
    }

    @Override
    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(DatabaseType<T, D> type, String namespace, Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException {
        return getOrCreateDatabase(type, namespace, classifier, DatabaseConfig.builder().build());
    }

    private @Nullable <T extends Object> T doRetryRequest(String url, Request req,
                                                          Function<Response, T> onSuccess) throws IOException {
        log.debug("Database was requested but not ready yet.");
        int retriesAmount = 10;
        long retriesDelaySeconds = 1;
        for (int i = 0; i <= retriesAmount; i++) {
            if (i == retriesAmount) {
                break;
            }
            try (Response checkDbIsReadyResponse = client.newCall(req).execute()) {
                if (checkDbIsReadyResponse.code() == 202) {
                    log.debug("Database was requested but not ready yet. Retrying");
                } else if (checkDbIsReadyResponse.isSuccessful()) {
                    return onSuccess.apply(checkDbIsReadyResponse);
                } else {
                    log.error("Failed to put to " + url + " with code " + checkDbIsReadyResponse.code() + ". Retrying");
                }
                Thread.sleep(retriesDelaySeconds * 1000);
            } catch (InterruptedException e) {
                log.error("Failed during timeout when sending request to " + url, e);
                Thread.currentThread().interrupt();
                throw new DbaasException(e);
            }
        }

        String message = "Has reached maximum number of retries amount";
        log.error(message);
        throw new DbaasException(message);
    }


    @Override
    @Nullable
    public <T, D extends AbstractDatabase<T>> D getDatabase(DatabaseType<T, D> type,
                                                            String namespace,
                                                            String userRole,
                                                            Map<String, Object> classifier)
            throws DbaasException, DbaasUnavailableException {
        log.info("Requesting get database of type {} and classifier {}",
                type.getName(), classifier);
        classifierChecker.check(classifier);
        Class<? extends D> databaseClass = type.getDatabaseClass();
        String url = address + DBAAS_BASE_URL + "/" + namespace + DATABASES + GET_BY_CLASSIFIER + "/" + type.getName();
        GetDatabaseByClassifierRequest getDatabaseByClassifierRequest = new GetDatabaseByClassifierRequest(classifier, userRole);
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JACK.writeValueAsBytes(getDatabaseByClassifierRequest), JSON)).build();
            try (Response response = client.newCall(req).execute()) {
                // HttpStatus Accepted(202) means that database was requested but not ready yet
                if (!response.isSuccessful()) {
                    checkDbaaSApiVersion(address);
                    String responseBody = response.body() != null ? response.body().string() : null;
                    String message = "Failed get database with classifier: " + classifier + " and type: " + type + " .Url: " + url + " code " + response.code() +
                            (responseBody != null ? (" and body: " + responseBody) : "");
                    log.error(message);
                    if (response.code() == 404 ) return null;
                    throw new DbaasException(message);
                }
                return JACK.readValue(response.body().byteStream(), databaseClass);
            }
        } catch (JsonProcessingException e) {
            log.error(MARSHALLING_FAILED + url, e);
            throw new DbaasException(e);
        } catch (IOException ioe) {
            log.error(FAILED + url, ioe);
            throw new DbaasException(ioe);
        }
    }

    @Override
    @Nullable
    public <T, D extends AbstractDatabase<T>> T getConnection(DatabaseType<T, D> type,
                                                              String namespace,
                                                              String userRole,
                                                              Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException {
        D database = getDatabase(type, namespace, userRole, classifier);
        return database != null ? database.getConnectionProperties() : null;
    }
}
