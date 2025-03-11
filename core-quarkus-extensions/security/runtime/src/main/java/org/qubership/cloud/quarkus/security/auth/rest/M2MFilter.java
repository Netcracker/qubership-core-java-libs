package org.qubership.cloud.quarkus.security.auth.rest;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import io.vertx.core.http.HttpHeaders;
import org.qubership.cloud.quarkus.security.auth.M2MManager;
import org.qubership.cloud.security.core.auth.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2MFilter implements ClientRequestFilter {
    public static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";
    private static final Logger LOGGER = LoggerFactory.getLogger(M2MFilter.class);

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        Token token = M2MManager.getInstance().getToken();
        clientRequestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION.toString(), AUTHORIZATION_HEADER_PREFIX + token.getTokenValue());
        LOGGER.debug("Authorization M2M header added");
    }
}
