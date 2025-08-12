package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SpringControlPlaneClient extends ControlPlaneClient {

    private final MicroserviceRestClient restClient;

    public SpringControlPlaneClient(String controlPlaneUrl, MicroserviceRestClient restClient) {
        super(controlPlaneUrl);
        this.restClient = restClient;
    }

    @Deprecated
    @Override
    public void postRoutes(RegistrationRequest registrationRequest) {
        sendRequest(registrationRequest);
    }

    public void sendRequest(CommonRequest request) {
        try {
            String url = controlPlaneUrl + request.getUrl();
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            Object payload = request.getPayload();
            log.info("{} request to {} started. Body: {}", method, url, payload);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON.toString()));

            RestClientResponseEntity<Object> response = restClient.doRequest(
                    url,
                    method,
                    headers,
                    payload,
                    Object.class);

            HttpStatus status = HttpStatus.valueOf(response.getHttpStatus());
            if (!status.is2xxSuccessful()) {
                throw new HttpServerErrorException(status, "Error during send request. " + response.getResponseBody());
            }
            log.info("{} request done successfully for {}.", method, url);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }
}
