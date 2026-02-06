package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.concurrent.TimeUnit;

import static io.fabric8.kubernetes.client.utils.HttpClientUtils.getHttpClientFactory;

@Priority
public class DefaultFabric8KubernetesClientBuilderAdapter implements Fabric8KubernetesClientBuilderAdapter {

    public static final String HTTP_CONNECT_TIMEOUT_SEC = "k8s.http.connect.timeout.sec";
    public static final int DEF_HTTP_CONNECT_TIMEOUT_SEC = 15;

    public KubernetesClientBuilder adapt(KubernetesClientBuilder clientBuilder) {
        return clientBuilder.withHttpClientFactory(getHttpClientFactory())
                .withHttpClientBuilderConsumer(builder ->
                        builder.connectTimeout(Integer.getInteger(HTTP_CONNECT_TIMEOUT_SEC, DEF_HTTP_CONNECT_TIMEOUT_SEC),
                                TimeUnit.SECONDS));
    }
}
