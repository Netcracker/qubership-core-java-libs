package org.qubership.cloud.consul.provider.spring.common;

import org.qubership.cloud.consul.provider.common.SimpleTokenStorageFactory;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.consul.provider.common.TokenStorageFactory;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.springframework.cloud.consul.config.ConsulConfigProperties;


public class SpringTokenStorageFactory extends SimpleTokenStorageFactory {
    private final ConsulConfigProperties consulConfigProperties;

    public SpringTokenStorageFactory(ConsulConfigProperties consulConfigProperties,
                                     MicroserviceRestClient restClient) {
        super(restClient);
        this.consulConfigProperties = consulConfigProperties;
    }

    @Override
    protected TokenStorage createTokenStorage(TokenStorageFactory.CreateOptions ignored) {
        return new ConfigBasedTokenStorage(consulConfigProperties);
    }
}
