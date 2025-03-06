package org.qubership.cloud.context.propagation.spring.webclient.interceptor;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;


public class SpringWebClientInterceptor implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction exchangeFunction) {
        return Mono.deferContextual(contextView -> {
            SpringResponseWebClientContextData springResponseContextData = new SpringResponseWebClientContextData();

            List<Object> contextObjects = CoreContextPropagator.getIfPresent(contextView);
            if (contextObjects != null ) {
                contextObjects.stream()
                        .filter(SerializableContext.class::isInstance)
                        .map(SerializableContext.class::cast)
                        .forEach(serializableContext -> serializableContext.serialize(springResponseContextData));
            } else {
                RequestContextPropagation.populateResponse(springResponseContextData);
            }

            return exchangeFunction.exchange(springResponseContextData.addHeadersToRequest(clientRequest));
        });
    }
}
