package org.qubership.cloud.quarkus.logging.manager.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

import static io.vertx.core.http.HttpMethod.GET;

public class LoggerHandler implements Handler<RoutingContext> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();

        HttpMethod method = request.method();

        if (GET == method) {
            handleGet(request, response);
        } else {
            response.end();
        }
    }

    private void handleGet(HttpServerRequest request, HttpServerResponse response) {
        response.headers().add("Content-Type", "application/json");
        Map<String, String> loggers = LogController.getLoggers();
        try {
            response.end(objectMapper.writeValueAsString(loggers));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
