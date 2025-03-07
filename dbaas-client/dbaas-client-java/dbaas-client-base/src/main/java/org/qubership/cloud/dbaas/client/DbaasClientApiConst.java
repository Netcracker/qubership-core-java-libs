package org.qubership.cloud.dbaas.client;

class DbaasClientApiConst {
    private DbaasClientApiConst() {
    }

    static final String DBAAS_BASE_URL = "/api/v3/dbaas";
    static final String PHYSICAL_DATABASES = "/physical_databases";
    static final String NAMESPACE = "namespace";
    static final String DATABASES = "/databases";
    public static final String GET_BY_CLASSIFIER = "/get-by-classifier";

    // urls
    static final String API_VERSION_ENDPOINT = "/api-version";
    static final String CREATE_DATABASE_TEMPLATE_ENDPOINT = DBAAS_BASE_URL + "/{namespace}" + DATABASES;
    static final String ASYNC_CREATE_DATABASE_TEMPLATE_ENDPOINT = CREATE_DATABASE_TEMPLATE_ENDPOINT + "?async=true";
    static final String GET_PHYSICAL_DATABASES_TEMPLATE_ENDPOINT = DBAAS_BASE_URL + "/{type}" + PHYSICAL_DATABASES;
    static final String GET_CONNECTION_TEMPLATE_ENDPOINT = DBAAS_BASE_URL + "/{namespace}" + DATABASES + GET_BY_CLASSIFIER + "/{type}";
    static final String HEALTH_ENDPOINT = "/health";
}
