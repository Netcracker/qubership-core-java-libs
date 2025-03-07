package org.qubership.cloud.consul.provider.common;

import org.qubership.cloud.consul.provider.common.client.ConsulClient;
import org.qubership.cloud.consul.provider.common.client.ConsulOkHttpClient;
import okhttp3.OkHttpClient;


public class OkHttpTokenStorageFactory extends TokenStorageFactory {

    private final OkHttpClient client;

    public OkHttpTokenStorageFactory(OkHttpClient client) {
        super();
        this.client = client;
    }

    @Override
    protected ConsulClient createTokenExchanger(CreateOptions config) {
        return new ConsulOkHttpClient(client, config.consulUrl, config.m2mSupplier);
    }

    @Override
    protected TokenStorage createTokenStorage(TokenStorageFactory.CreateOptions createOptions) {
        return new SimpleTokenStorage();
    }

}
