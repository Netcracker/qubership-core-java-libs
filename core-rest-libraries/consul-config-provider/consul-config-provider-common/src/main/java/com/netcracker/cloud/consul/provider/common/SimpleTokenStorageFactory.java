package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulRestClient;
import com.netcracker.cloud.restclient.MicroserviceRestClient;

/*
    @Deprecated.
    The usage of {@link SimpleTokenStorageFactory} can cause issues.
    Use {@link com.netcracker.cloud.consul.provider.common.OkHttpTokenStorageFactory} instead.
 */
@Deprecated
public class SimpleTokenStorageFactory extends TokenStorageFactory {

    private final MicroserviceRestClient restClient;

    public SimpleTokenStorageFactory(MicroserviceRestClient restClient) {
        super();
        this.restClient = restClient;
    }

    @Override
    protected TokenStorage createTokenStorage(CreateOptions ignored) {
        return new SimpleTokenStorage();
    }

    @Override
    protected ConsulClient createTokenExchanger(CreateOptions config) {
        return new ConsulRestClient(restClient, config.consulUrl, config.m2mSupplier);
    }
}
