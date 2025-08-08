package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.entity.DatabaseCreateRequest;
import org.qubership.cloud.dbaas.client.entity.GetDatabaseByClassifierRequest;
import org.qubership.cloud.dbaas.client.entity.PhysicalDatabases;
import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import org.qubership.cloud.dbaas.client.exceptions.DatabaseNotReadyException;
import org.qubership.cloud.dbaas.client.exceptions.DbaasException;
import org.qubership.cloud.dbaas.client.exceptions.DbaasUnavailableException;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.service.ClassifierChecker;
import org.qubership.cloud.dbaas.client.util.RetryTemplateUtils;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientException;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.qubership.cloud.dbaas.client.DbaasClientApiConst.*;

@Slf4j
public class DbaasClientImpl implements DbaasClient {

    private final MicroserviceRestClient dbaasRestClient;
    private final RetryTemplate retryTemplate;
    private final RetryTemplate awaitAsyncDbCreationRetryTemplate;
    private final String dbaasAgentHost;
    private final ClassifierChecker classifierChecker;
    private static final String RECEIVED_RESPONSE = "Received response {}";

    public DbaasClientImpl(MicroserviceRestClient dbaasRestClient, String dbaasAgentHost) {
        this(dbaasRestClient, null, dbaasAgentHost);
    }

    public DbaasClientImpl(MicroserviceRestClient dbaasRestClient, RetryTemplate retryTemplate, String dbaasAgentHost) {
        this(dbaasRestClient, retryTemplate, null, dbaasAgentHost);
    }

    public DbaasClientImpl(MicroserviceRestClient dbaasRestClient, RetryTemplate retryTemplate, RetryTemplate awaitAsyncDbCreationRetryTemplate, String dbaasAgentHost) {
        Objects.requireNonNull(dbaasRestClient);
        Objects.requireNonNull(dbaasAgentHost);
        if (retryTemplate == null) {
            retryTemplate = getDefaultRetryTemplate();
        }
        if (awaitAsyncDbCreationRetryTemplate == null) {
            awaitAsyncDbCreationRetryTemplate = RetryTemplateUtils.createAwaitAsyncDbCreationRetryTemplate(20 * 60);
        }
        this.dbaasRestClient = dbaasRestClient;
        this.retryTemplate = retryTemplate;
        this.dbaasAgentHost = dbaasAgentHost;
        this.awaitAsyncDbCreationRetryTemplate = awaitAsyncDbCreationRetryTemplate;
        classifierChecker = new ClassifierChecker();
    }

    private RetryTemplate getDefaultRetryTemplate() {
        RetryTemplate defaultRetryTemplate;
        defaultRetryTemplate = new RetryTemplate();
        defaultRetryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        defaultRetryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        return defaultRetryTemplate;
    }

    private void rethrowException(MicroserviceRestClientException restException, String msg)
            throws DbaasException, DbaasUnavailableException {
        log.error(msg + " {}", restException);
        if (!isAvailable()) {
            throw new DbaasUnavailableException(restException);
        } else {
            throw new DbaasException(restException);
        }
    }

    @Override
    @Nullable
    public PhysicalDatabases getPhysicalDatabases(String type)
            throws DbaasException, DbaasUnavailableException {
        URI uri = new UriTemplate(dbaasAgentHost + GET_PHYSICAL_DATABASES_TEMPLATE_ENDPOINT).expand(type);
        try {
            return this.retryTemplate.execute(
                    context -> getPhysicalDatabaseExecuteRequest(uri));
        } catch (MicroserviceRestClientException restException) {
            rethrowException(restException, "Caught exception during invocation of getPhysicalDatabases()");
        }
        return null;
    }

    private PhysicalDatabases getPhysicalDatabaseExecuteRequest(URI uri) {
        log.debug("Get physical databases from {}", uri);
        RestClientResponseEntity<PhysicalDatabases> responseEntity = dbaasRestClient.doRequest(uri, HttpMethod.GET,
                null,
                null,
                PhysicalDatabases.class);
        PhysicalDatabases databases = responseEntity.getResponseBody();
        log.debug("Received response {} with {} identified physical databases",
                responseEntity,
                databases.getIdentified().size());
        return databases;
    }

    @Override
    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(DatabaseType<T, D> type, String namespace, Map<String, Object> classifier, DatabaseConfig databaseConfig) throws DbaasException, DbaasUnavailableException {
        classifierChecker.check(classifier);
        DatabaseCreateRequest dbCreateRequest = new DatabaseCreateRequest(classifier, type.getName(), databaseConfig);
        Class<? extends D> databaseClass = type.getDatabaseClass();
        try {
            URI uri = new UriTemplate(dbaasAgentHost + ASYNC_CREATE_DATABASE_TEMPLATE_ENDPOINT).expand(namespace);
            String uriString = uri.toString();
            return this.retryTemplate.execute(
                    context -> executeCreateDbRequest(uriString, dbCreateRequest, databaseClass)
            );
        } catch (MicroserviceRestClientException restException) {
            rethrowException(restException, "Caught exception during invocation of createDatabase()");
        }
        throw new DbaasException("database was not created for unknown reasons");
    }

    @Override
    public <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(DatabaseType<T, D> type, String namespace, Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException {
        return getOrCreateDatabase(type, namespace, classifier, DatabaseConfig.builder().build());
    }

    private <T, D extends AbstractDatabase<T>> D executeCreateDbRequest(String uri, DatabaseCreateRequest dbCreateRequest, Class<? extends D> databaseClass) {
        RestClientResponseEntity<? extends D> response = awaitAsyncDbCreationRetryTemplate.execute(context -> {
            log.debug("Sending create request: {} to URI: {}", dbCreateRequest, uri);
            RestClientResponseEntity<? extends D> responseEntity = dbaasRestClient.doRequest(uri,
                    HttpMethod.PUT,
                    null,
                    dbCreateRequest,
                    databaseClass);
            log.debug(RECEIVED_RESPONSE, responseEntity);
            if (responseEntity.getHttpStatus() == HttpStatus.ACCEPTED.value()) {
                log.debug("Database was requested but not ready yet.");
                throw new DatabaseNotReadyException();
            }
            return responseEntity;
        });
        return response.getResponseBody();
    }

    @Nullable
    public <T, D extends AbstractDatabase<T>> D getDatabase(final DatabaseType<T, D> type,
                                                            final String namespace,
                                                            String userRole,
                                                            final Map<String, Object> classifier)
            throws DbaasException, DbaasUnavailableException {
        Class<? extends D> databaseClass = type.getDatabaseClass();
        HashMap<String, Object> params = new HashMap<>();
        params.put(NAMESPACE, namespace);
        params.put("type", type.getName());
        URI uri = new UriTemplate(dbaasAgentHost + GET_CONNECTION_TEMPLATE_ENDPOINT).expand(params);
        try {
            return this.retryTemplate.execute(
                    context -> executeGetDatabaseRequest(uri, classifier, userRole, databaseClass)
            );
        } catch (MicroserviceRestClientException restException) {
            rethrowException(restException, "Caught exception during invocation of getConnection()");
        }
        return null;
    }

    @Nullable
    public <T, D extends AbstractDatabase<T>> T getConnection(final DatabaseType<T, D> type,
                                                              final String namespace,
                                                              final String userRole,
                                                              final Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException {
        D database = getDatabase(type, namespace, userRole, classifier);
        return database != null ? database.getConnectionProperties() : null;
    }

    private <T, D extends AbstractDatabase<T>> D executeGetDatabaseRequest(URI uri, final Map<String, Object> classifier, String role, Class<? extends D> databaseClass) {
        log.debug("Sending get database request: classifier: {}, role: {} to URI: {}", classifier, role, uri);
        GetDatabaseByClassifierRequest getDatabaseByClassifier = new GetDatabaseByClassifierRequest(classifier, role);
        try {
            RestClientResponseEntity<? extends D> responseEntity = dbaasRestClient.doRequest(uri,
                    HttpMethod.POST,
                    null,
                    getDatabaseByClassifier,
                    databaseClass);
            log.debug(RECEIVED_RESPONSE, responseEntity);
            return responseEntity.getResponseBody() != null ? responseEntity.getResponseBody() : null;
        } catch (MicroserviceRestClientResponseException e) {
            // when connection is not found, dbaas-aggregator sends 404 response
            if (HttpStatus.NOT_FOUND.value() == (e.getHttpStatus())) {
                return null;
            }
            throw e;
        }
    }

    public boolean isAvailable() {
        try {
            URI isAvailableUri = URI.create(dbaasAgentHost + HEALTH_ENDPOINT);
            log.debug("Sending isAvailable request to URI: {}", isAvailableUri);
            RestClientResponseEntity<Map> responseEntity = dbaasRestClient.doRequest(isAvailableUri,
                    HttpMethod.GET,
                    null,
                    null,
                    Map.class);
            log.debug(RECEIVED_RESPONSE, responseEntity);
            return "UP".equals(responseEntity.getResponseBody().get("status"));
        } catch (Exception e) {
            log.error("Caught exception while checking for availability {}", e);
            return false;
        }
    }

    @Override
    public String toString() {
        return "DbaasClientImpl{" +
                "dbaasRestClient=" + dbaasRestClient +
                ", retryTemplate=" + retryTemplate +
                ", dbaasAgentHost='" + dbaasAgentHost + '\'' +
                '}';
    }
}
