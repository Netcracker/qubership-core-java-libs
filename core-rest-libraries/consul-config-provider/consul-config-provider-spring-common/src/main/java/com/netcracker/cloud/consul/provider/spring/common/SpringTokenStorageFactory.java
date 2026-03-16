package com.netcracker.cloud.consul.provider.spring.common;

import com.netcracker.cloud.consul.provider.common.SimpleTokenStorageFactory;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
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
