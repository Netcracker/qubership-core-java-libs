package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulOkHttpClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OkHttpTokenStorageFactoryTest {

    public static final String CONSUL_ADDRESS = "http://consul:8301";

    @Test
    void OkHttpTokenStorageFactoryMethodsTest() {
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        TokenStorageFactory tokenStorageFactory = new OkHttpTokenStorageFactory(okHttpClient);

        TokenStorage tokenStorage = tokenStorageFactory.createTokenStorage(null);
        assertTrue(tokenStorage instanceof SimpleTokenStorage);

        TokenStorageFactory.CreateOptions createOptions = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_ADDRESS).namespace("test-namespace").m2mSupplier(() -> "").build();
        ConsulClient consulClient = tokenStorageFactory.createTokenExchanger(createOptions);
        assertTrue(consulClient instanceof ConsulOkHttpClient);
    }
}
