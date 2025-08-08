package com.netcracker.maas.declarative.kafka.quarkus.client.config;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.impl.InternalTenantServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl.MaasKafkaTopicServiceProviderImpl;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;


@Singleton
public class MaasKafkaProdClientConfig {
    @Singleton
    @Produces
    KafkaMaaSClient kafkaMaaSClient(MaasKafkaProps props, M2MManager m2mManager) {
        HttpClient httpClient = new HttpClient(() -> m2mManager.getToken().getTokenValue());
        return new KafkaMaaSClientImpl(
                httpClient,
                () -> new TenantManagerConnectorImpl(httpClient),
                new ApiUrlProvider(new ServerApiVersion(httpClient, props.maasAgentUrl), props.maasAgentUrl)
        );
    }

    @Singleton
    @Produces
    MaasKafkaTopicServiceProvider maasKafkaTopicServiceProvider(KafkaMaaSClient kafkaMaaSClient) {
        return new MaasKafkaTopicServiceProviderImpl(
                kafkaMaaSClient
        );
    }

    @Singleton
    @Produces
    @DefaultBean
    InternalTenantService internalTenantService(M2MManager m2mManager) {
        HttpClient httpClient = new HttpClient(() -> m2mManager.getToken().getTokenValue());
        TenantManagerConnectorImpl tenantManagerConnector = new TenantManagerConnectorImpl(httpClient);
        return new InternalTenantServiceImpl(tenantManagerConnector);
    }
}
