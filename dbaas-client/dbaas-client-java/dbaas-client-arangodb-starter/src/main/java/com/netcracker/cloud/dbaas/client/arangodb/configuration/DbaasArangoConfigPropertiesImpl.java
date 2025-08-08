package org.qubership.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.Protocol;
import com.arangodb.config.ArangoConfigProperties;
import com.arangodb.config.HostDescription;
import com.arangodb.entity.LoadBalancingStrategy;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class DbaasArangoConfigPropertiesImpl implements ArangoConfigProperties {

    @NonNull
    private Map<String, String> properties;

    @Override
    public Optional<List<HostDescription>> getHosts() {
        return Optional.ofNullable(properties.get("hosts"))
                .map(s -> {
                    List<HostDescription> hostDescriptions = new ArrayList<>();
                    String[] hosts = s.split(",");
                    for (String host : hosts) {
                        hostDescriptions.add(HostDescription.parse(host));
                    }
                    return hostDescriptions;
                });
    }

    @Override
    public Optional<Protocol> getProtocol() {
        return Optional.ofNullable(properties.get("protocol")).map(Protocol::valueOf);
    }

    @Override
    public Optional<String> getUser() {
        return Optional.ofNullable(properties.get("user"));
    }

    @Override
    public Optional<String> getPassword() {
        return Optional.ofNullable(properties.get("password"));
    }

    @Override
    public Optional<String> getJwt() {
        return Optional.ofNullable(properties.get("jwt"));
    }

    @Override
    public Optional<Integer> getTimeout() {
        return Optional.ofNullable(properties.get("timeout")).map(Integer::valueOf);
    }

    @Override
    public Optional<Boolean> getUseSsl() {
        return Optional.ofNullable(properties.get("useSsl")).map(Boolean::valueOf);
    }

    @Override
    public Optional<Boolean> getVerifyHost() {
        return Optional.ofNullable(properties.get("verifyHost")).map(Boolean::valueOf);
    }

    @Override
    public Optional<Integer> getChunkSize() {
        return Optional.ofNullable(properties.get("chunkSize")).map(Integer::valueOf);
    }

    @Override
    public Optional<Integer> getMaxConnections() {
        return Optional.ofNullable(properties.get("maxConnections")).map(Integer::valueOf);
    }

    @Override
    public Optional<Long> getConnectionTtl() {
        return Optional.ofNullable(properties.get("connectionTtl")).map(Long::valueOf);
    }

    @Override
    public Optional<Integer> getKeepAliveInterval() {
        return Optional.ofNullable(properties.get("keepAliveInterval")).map(Integer::valueOf);
    }

    @Override
    public Optional<Boolean> getAcquireHostList() {
        return Optional.ofNullable(properties.get("acquireHostList")).map(Boolean::valueOf);
    }

    @Override
    public Optional<Integer> getAcquireHostListInterval() {
        return Optional.ofNullable(properties.get("acquireHostListInterval")).map(Integer::valueOf);
    }

    @Override
    public Optional<LoadBalancingStrategy> getLoadBalancingStrategy() {
        return Optional.ofNullable(properties.get("loadBalancingStrategy")).map(LoadBalancingStrategy::valueOf);
    }

    @Override
    public Optional<Integer> getResponseQueueTimeSamples() {
        return Optional.ofNullable(properties.get("responseQueueTimeSamples")).map(Integer::valueOf);
    }
}
