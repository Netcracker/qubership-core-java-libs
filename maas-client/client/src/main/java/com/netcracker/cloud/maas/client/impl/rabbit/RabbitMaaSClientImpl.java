package org.qubership.cloud.maas.client.impl.rabbit;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import org.qubership.cloud.maas.client.api.rabbit.VHost;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.dto.rabbit.v1.VHostRequest;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.*;

@Slf4j
public class RabbitMaaSClientImpl implements RabbitMaaSClient {
    private final HttpClient restClient;
    private final ApiUrlProvider apiProvider;

    public RabbitMaaSClientImpl(HttpClient restClient, ApiUrlProvider apiProvider) {
        this.restClient = restClient;
        this.apiProvider = apiProvider;
    }

    @Override
    @Deprecated
    public VHost getVHost(String name) {
        return getVHost(new Classifier(name));
    }

    @Override
    @Deprecated
    public VHost getVHost(String name, String tenantId) {
        return getVHost(new Classifier(name).tenantId(tenantId));
    }

    @Override
    @Deprecated
    public VHost getVHost(Classifier classifier) {
        log.info("Get vhost by: {}", classifier);
        return restClient.request(apiProvider.getRabbitVhostUrl(true))
                .post(new VHostRequest(classifier))
                .expect(HTTP_OK, HTTP_CREATED)
                .sendAndReceive(VHost.class)
                .orElse(null);
    }

    @Override
    public VHost getOrCreateVirtualHost(Classifier classifier) {
        log.info("Get vhost by: {}", classifier);
        return restClient.request(apiProvider.getRabbitVhostUrl(true))
                .post(new VHostRequest(classifier))
                .expect(HTTP_OK, HTTP_CREATED)
                .sendAndReceive(VHost.class)
                .orElse(null);
    }

    @Override
    public VHost getVirtualHost(Classifier classifier) {
        log.info("Get vhost by: {}", classifier);
        return restClient.request(apiProvider.getRabbitVhostGetByClassifierUrl(true))
                .post(classifier)
                .expect(HTTP_OK)
                .supressError(HTTP_NOT_FOUND, body -> log.info("Virtual Host not found by {}", classifier))
                .sendAndReceive(VHostAndConfig.class)
                .map(VHostAndConfig::getVhost)
                .orElse(null);
    }

    @Override
    public String getVersionedQueueName(String queueName, BlueGreenStatePublisher publisher) {
        return queueName + "-" + publisher.getBlueGreenState().getCurrent().getVersion().toString();
    }

    @Override
    public List<String> getExchangeNames(String exchangeName) {
        return Collections.singletonList(exchangeName);
    }


    @Data
    static class VHostAndConfig {
        VHost vhost;
        Map<String, Object> entities;
    }
}

