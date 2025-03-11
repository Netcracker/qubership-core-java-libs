package org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import io.vertx.core.json.Json;
import okhttp3.*;
import org.jboss.logging.Logger;

import java.io.IOException;

public class QuarkusControlPlaneClient extends ControlPlaneClient {

    private static final Logger LOGGER = Logger.getLogger(QuarkusControlPlaneClient.class.getName());

    private final OkHttpClient restClient;

    public QuarkusControlPlaneClient(String controlPlaneUrl, OkHttpClient restClient) {
        super(controlPlaneUrl);
        this.restClient = restClient;
    }

    @Deprecated
    @Override
    public void postRoutes(RegistrationRequest registrationRequest) {
        sendRequest(registrationRequest);
    }

    @Override
    public void sendRequest(CommonRequest request) {
        String url = controlPlaneUrl + request.getUrl();
        String method = request.getMethod();
        Object payload = request.getPayload();
        LOGGER.info(method + " request to " + url + " started. Body: " + payload);
        Response response;
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), Json.encode(payload));
            response = restClient.newCall(new Request.Builder()
                    .method(method, requestBody)
                    .url(url)
                    .build()
            ).execute();
        } catch (IOException e) {
            LOGGER.error("Error during send request: ", e);
            throw new RuntimeException(e);
        }

        try {
            if (!response.isSuccessful()) {
                String msg = "Error during request: " + response.message();
                LOGGER.error(msg);
                throw new RuntimeException(msg);
            }
            LOGGER.info(method + " request done successfully for " + url);
        } finally {
            response.close();
        }
    }
}
