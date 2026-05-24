package com.netcracker.cloud.maas.client.impl;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import com.netcracker.cloud.maas.client.impl.rabbit.RabbitMaaSClientImpl;
import com.netcracker.cloud.tenantmanager.client.TenantManagerConnector;
import com.netcracker.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;

import java.util.function.Supplier;

public class MaaSAPIClientImpl implements MaaSAPIClient {
    private final Lazy<TenantManagerConnector> tenantManagerConnector;
    private final HttpClient restClient;
    private final ServerApiVersion serverApiVersion;
    private final ApiUrlProvider apiProvider;

    public MaaSAPIClientImpl(Supplier<String> tokenSupplier, boolean k8sEnabled) {
        this.restClient = HttpClient.getMaasClient(tokenSupplier, k8sEnabled);
        this.serverApiVersion = new ServerApiVersion(restClient, Env.apiUrl(k8sEnabled));
        this.tenantManagerConnector = new Lazy<>(() -> new TenantManagerConnectorImpl(HttpClient.getM2mClient(tokenSupplier, k8sEnabled)));
        this.apiProvider = new ApiUrlProvider(serverApiVersion, Env.apiUrl(k8sEnabled));
    }

    public MaaSAPIClientImpl(Supplier<String> tokenSupplier, boolean k8sEnabled, TenantManagerConnector tenantManagerConnector, BlueGreenStatePublisher statePublisher) {
        this.restClient = HttpClient.getMaasClient(tokenSupplier, k8sEnabled);
        this.serverApiVersion = new ServerApiVersion(restClient, Env.apiUrl(k8sEnabled));
        this.tenantManagerConnector = new Lazy<>(() -> tenantManagerConnector);
        this.apiProvider = new ApiUrlProvider(serverApiVersion, Env.apiUrl(k8sEnabled));
    }

    @Override
    public KafkaMaaSClient getKafkaClient() {
        return new KafkaMaaSClientImpl(restClient, tenantManagerConnector, apiProvider);
    }

    @Override
    public RabbitMaaSClient getRabbitClient() {
        return new RabbitMaaSClientImpl(restClient, apiProvider);
    }

    @Override
    public void close() throws Exception {
        if (tenantManagerConnector.isInitialized()) {
            tenantManagerConnector.get().close();
        }
    }
}
