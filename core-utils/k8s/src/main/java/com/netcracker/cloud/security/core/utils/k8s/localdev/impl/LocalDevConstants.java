package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Shared constants for local-dev kubeconfig / OIDC / TokenRequest code.
 * <p>
 * Media types are defined here because k8s-utils has no JAX-RS / Spring dependency
 * ({@code jakarta.ws.rs.core.MediaType} is not on the classpath).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LocalDevConstants {

    static final String MICROSERVICE_NAME_PROPERTY = "cloud.microservice.name";
    static final String MICROSERVICE_NAME_ENV = "CLOUD_MICROSERVICE_NAME";
    static final String NAMESPACE_ENV = "CLOUD_NAMESPACE";

    static final String APPLICATION_JSON = "application/json";
    static final String APPLICATION_JWK_SET_JSON = "application/jwk-set+json";
    static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String ACCEPT_HEADER = "Accept";
    static final String BEARER_PREFIX = "Bearer ";

    static final String WELL_KNOWN_OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";

    static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    static final long TOKEN_REQUEST_EXPIRATION_SECONDS = 28800L; // 8 hours

    static final String OIDC_AUTH_PROVIDER_NAME = "oidc";

    static final String TOKEN_REQUEST_API_VERSION = "authentication.k8s.io/v1";
    static final String TOKEN_REQUEST_KIND = "TokenRequest";
    static final String TOKEN_REQUEST_SPEC_AUDIENCES = "audiences";
    static final String TOKEN_REQUEST_SPEC_EXPIRATION_SECONDS = "expirationSeconds";

    static final String K8S_TOKEN_STATUS_TOKEN = "token";
    static final String K8S_TOKEN_STATUS_EXPIRATION = "expirationTimestamp";

    static final String OIDC_DISCOVERY_TOKEN_ENDPOINT = "token_endpoint";
    static final String OIDC_DISCOVERY_ISSUER = "issuer";
    static final String OIDC_TOKEN_ID_TOKEN = "id_token";
    static final String OIDC_TOKEN_ACCESS_TOKEN = "access_token";
    static final String OIDC_GRANT_REFRESH_TOKEN = "refresh_token";
    static final String OIDC_FORM_CLIENT_ID = "client_id";
    static final String OIDC_FORM_CLIENT_SECRET = "client_secret";
    static final String OIDC_FORM_GRANT_TYPE = "grant_type";

    static final int MAX_ERROR_BODY_LENGTH = 500;

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class KubeConfigFields {
        static final String CURRENT_CONTEXT = "current-context";
        static final String CONTEXTS = "contexts";
        static final String CLUSTERS = "clusters";
        static final String USERS = "users";
        static final String CONTEXT = "context";
        static final String CLUSTER = "cluster";
        static final String USER = "user";
        static final String NAME = "name";
        static final String SERVER = "server";
        static final String TOKEN = "token";
        static final String CERTIFICATE_AUTHORITY = "certificate-authority";
        static final String CERTIFICATE_AUTHORITY_DATA = "certificate-authority-data";
        static final String INSECURE_SKIP_TLS_VERIFY = "insecure-skip-tls-verify";
        static final String AUTH_PROVIDER = "auth-provider";
        static final String EXEC = "exec";
        static final String STATUS = "status";
        static final String CONFIG = "config";
        static final String ID_TOKEN = "id-token";
        static final String ACCESS_TOKEN = "access-token";
        static final String REFRESH_TOKEN = "refresh-token";
        static final String IDP_ISSUER_URL = "idp-issuer-url";
        static final String CLIENT_ID = "client-id";
        static final String CLIENT_SECRET = "client-secret";
    }
}
