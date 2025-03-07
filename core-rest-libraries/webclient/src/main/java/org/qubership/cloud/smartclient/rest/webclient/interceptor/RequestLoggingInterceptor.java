package org.qubership.cloud.smartclient.rest.webclient.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Prints out request parameters upon receiving a response from WebClient
 */
public class RequestLoggingInterceptor implements ExchangeFilterFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return ExchangeFilterFunction
                .ofResponseProcessor(clientResponse -> log(request, clientResponse))
                .filter(request, next);
    }

    private Mono<ClientResponse> log(ClientRequest request, ClientResponse response) {
        return Mono.just(response)
                .doOnNext(r ->
                        LOGGER.debug("Request: " +
                                        "\n\turl={}" +
                                        "\n\tmethod={}" +
                                        "\n\theaders={}",
                                request.url(),
                                request.method(),
                                request.headers().keySet()
                        ))
                .doOnNext(r ->
                        LOGGER.debug("Response: " +
                                        "\n\tstatus code={}" +
                                        "\n\theader={}",
                                response.statusCode(),
                                response.headers().asHttpHeaders().keySet()
                        ));
    }
}
