package com.netcracker.cloud.junit.cloudcore.extension.callbacks.classes;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Cloud;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.IntValue;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.PortForward;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.Value;
import com.netcracker.cloud.junit.cloudcore.extension.service.NetSocketAddress;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;

import java.net.URI;
import java.net.URL;

public class TestClass {

    @Getter
    public static class TestKubernetesClient {
        @Cloud(cloud = @Value(prop = "clouds.cloud_1.name"), namespace = @Value(prop = "clouds.cloud_1.namespaces.origin"))
        KubernetesClient cloud1KubernetesClient;
        @Cloud(cloud = @Value(prop = "clouds.cloud_2.name"), namespace = @Value(prop = "clouds.cloud_2.namespaces.origin"))
        KubernetesClient cloud2KubernetesClient;
    }

    @Getter
    public static class TestUri {
        @PortForward(serviceName = @Value(value = "service"), protocol = @Value(value = "ftp"))
        URI uri;
    }

    @Getter
    public static class TestUrl {
        @PortForward(serviceName = @Value(value = "service"), protocol = @Value(value = "https"))
        URL url;
    }

    @Getter
    public static class TestPortForwardService {
        @Cloud
        PortForwardService portForwardService;
    }

    @Getter
    public static class TestSocketAddress {
        @PortForward(serviceName = @Value(value = "postgres"), port = @IntValue(5432),
                cloud = @Cloud(cloud = @Value(prop = "clouds.cloud_1.name"), namespace = @Value(prop = "clouds.cloud_1.namespaces.origin")))
        NetSocketAddress address;
    }
}
