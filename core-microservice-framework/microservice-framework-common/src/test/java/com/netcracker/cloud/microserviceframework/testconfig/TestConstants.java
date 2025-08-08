package org.qubership.cloud.microserviceframework.testconfig;

import org.apache.commons.lang3.StringUtils;

public class TestConstants {

    public final static String TEST_SWAGGER_FILES_DIRECTORY_PATH = "swagger-security-test";
    public final static String PETS_SWAGGER_JSON_FILE= TEST_SWAGGER_FILES_DIRECTORY_PATH + "/swagger-pets.json";
    public final static String POLICY_FILE = TEST_SWAGGER_FILES_DIRECTORY_PATH + "/policies.conf";
    public final static String DEFAULT_POLICY_FILE = TEST_SWAGGER_FILES_DIRECTORY_PATH + "/default-policies.conf";
    public final static String SWAGGER_EXPECTED_FILE = TEST_SWAGGER_FILES_DIRECTORY_PATH + "/expected-result.txt";

    public final static String TEST_ERROR_MESSAGE = "Test error message";
    public final static String TEST_ERROR_CODE = "error.test_massage";
    public final static String TEST_MESSAGE = "Test message";
    public final static String TEST_CODE = "test_massage";
    public final static String WRONG_TEST_CODE = "wrong.test.code";

    public static final String TEST_MONGO_PACKAGE = "org.qubership.cloud";
    public static final String MS_CHANGE_LOGS_PACKAGE = "org.qubership.changelogpackage";
    public static final String TENANT_AWARE_CHANGE_LOGS_PACKAGE = "org.qubership.tenantawarepackage";
    public static final String MS_CUSTOM_PACKAGE = "org.qubership.test-cloud.ms";
    public static final String TENANT_AWARE_CUSTOM_PACKAGE = "org.qubership.test-cloud.ms";
    public static final String NULL_PACKAGE = null;
    public static final String MONGO_DB_NAME = "admin";

    public final static char URI_DELIMITER = '/';
    public final static String MICROSERVICE_NAME = "microserviceName";
    public final static String TENANT_ID = "tenantId";
    public final static String DB_CLASSIFIER = "dbClassifier";
    public final static String DB_NAME = "dbName";

    public final static String DB_USER = "dbaas";
    public final static String AUTH_DB_NAME = "admin";
    public final static String DB_PASSWORD = "dbaas";
    public final static String MONGO_ROLE = "ROLE_readWrite";

    public final static String DISCOVERY_PROPERTY_NAME = "spring.cloud.discovery.client.health-indicator.enabled";
    public final static String FALSE_VALUE = "false";
    public final static String TRUE_VALUE = "true";
    public final static String DISCOVERY_PROPERTY = DISCOVERY_PROPERTY_NAME + "=" + TRUE_VALUE;

    static final String API_PATH = "/api/v1";
    static final String MONGO_DB_REGISTRATIONS_PATH = API_PATH + "/mongo-db-registrations";

    public final static String TEST_URI = StringUtils.join(new String[]{MONGO_DB_REGISTRATIONS_PATH,
            MICROSERVICE_NAME, TENANT_ID, DB_CLASSIFIER}, URI_DELIMITER);

    public final static String TENANT_TEST_URI = "http://tenant-manager:8080";

    public final static String MONGO_TEST_URI = "mongodb://"
    + DB_USER + ":" + DB_PASSWORD + "@localhost:27017,localhost:27018/";
}
