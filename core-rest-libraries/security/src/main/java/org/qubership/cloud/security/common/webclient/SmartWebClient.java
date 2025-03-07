package org.qubership.cloud.security.common.webclient;

import org.springframework.web.reactive.function.client.WebClient;

public interface SmartWebClient {
    WebClient.Builder getWebClientBuilderForMode(Mode mode);

    WebClient.Builder getWebClientBuilder();

    WebClient getWebClientForM2mAuthorization();

    WebClient getWebClientForUserAuthorization();
}

