package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VertxRequestContextData implements IncomingContextData {

    private final HttpServerRequest httpServerRequest;

    public VertxRequestContextData(HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
        httpServerRequest.headers().set("cloud-core.context-propagation.url", httpServerRequest.absoluteURI());
    }

    @Override
    public Object get(String name) {
        return httpServerRequest.headers().get(name);
    }

    @Override
    public Map<String, List<?>> getAll() {
        MultiMap headers = this.httpServerRequest.headers();
        return headers.names().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        headers::getAll
                ));
    }
}
