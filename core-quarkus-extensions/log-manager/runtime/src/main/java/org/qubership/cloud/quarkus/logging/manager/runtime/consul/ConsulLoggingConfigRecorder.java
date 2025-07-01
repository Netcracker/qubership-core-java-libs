package org.qubership.cloud.quarkus.logging.manager.runtime.consul;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;

@Recorder
public class ConsulLoggingConfigRecorder {

    public RuntimeValue<ConsulLoggingConfigWatchFactory> createConsulLoggingWatchFactory() {
        ConsulClient consulClient = CDI.current().select(ConsulClient.class).get();
        TokenStorage tokenStorage = CDI.current().select(TokenStorage.class).get();
        String namespace = ConfigProvider.getConfig().getValue("cloud.microservice.namespace", String.class);
        String microservice = ConfigProvider.getConfig().getValue("cloud.microservice.name", String.class);

        ConsulLoggingConfigWatchFactory factory = new ConsulLoggingConfigWatchFactory(consulClient, tokenStorage, namespace, microservice);
        return new RuntimeValue<>(factory);
    }
}
