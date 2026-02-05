package com.netcracker.cloud.junit.cloudcore.extension.client;

import io.fabric8.kubernetes.client.ConfigBuilder;

public interface Fabric8ConfigBuilderAdapter {

    ConfigBuilder adapt(ConfigBuilder configBuilder);
}
