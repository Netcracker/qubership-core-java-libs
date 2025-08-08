package com.netcracker.cloud.maas.client.impl;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.qubership.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import org.qubership.cloud.maas.client.impl.rabbit.RabbitMaaSClientImpl;
import org.qubership.cloud.tenantmanager.client.TenantManagerConnector;
import org.qubership.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;

import java.util.function.Supplier;

public class MaaSAPIClientImpl implements MaaSAPIClient {
    private final Lazy<TenantManagerConnector> tenantManagerConnector;
    private final HttpClient restClient;
    private final ServerApiVersion serverApiVersion;
    private final ApiUrlProvider apiProvider;

    public MaaSAPIClientImpl(Supplier<String> tokenSupplier) {
        this.restClient = new HttpClient(tokenSupplier);
        this.serverApiVersion = new ServerApiVersion(restClient, Env.apiUrl());
        this.tenantManagerConnector = new Lazy<>(() -> new TenantManagerConnectorImpl(restClient));
        this.apiProvider = new ApiUrlProvider(serverApiVersion, Env.apiUrl());
    }

    public MaaSAPIClientImpl(Supplier<String> tokenSupplier, TenantManagerConnector tenantManagerConnector, BlueGreenStatePublisher statePublisher) {
        this.restClient = new HttpClient(tokenSupplier);
        this.serverApiVersion = new ServerApiVersion(restClient, Env.apiUrl());
        this.tenantManagerConnector = new Lazy<>(() -> tenantManagerConnector);
        this.apiProvider = new ApiUrlProvider(serverApiVersion, Env.apiUrl());
    }

    @Override
    public KafkaMaaSClient getKafkaClient() {
        return new KafkaMaaSClientImpl(restClient, tenantManagerConnector, apiProvider);
    }

    @Override
    public RabbitMaaSClient getRabbitClient() {
        return new RabbitMaaSClientImpl(restClient, apiProvider);
    }
}
