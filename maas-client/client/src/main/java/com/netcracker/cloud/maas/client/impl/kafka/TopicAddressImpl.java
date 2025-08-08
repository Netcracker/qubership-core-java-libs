package com.netcracker.cloud.maas.client.impl.kafka;

import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.cloud.maas.client.api.kafka.TopicUserCredentials;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicUserCredentialsImpl;
import com.netcracker.cloud.maas.client.impl.kafka.protocolextractors.Extractor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TopicAddressImpl implements TopicAddress {
    private final TopicInfo info;
    private final Map<String, TopicUserCredentials> credentials;

    public TopicAddressImpl(TopicInfo info) {
        Objects.requireNonNull(info);

        this.info = info;
        this.credentials = Optional.ofNullable(info.getCredential())
                .map(credentials -> credentials.get("client"))
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(
                        TopicUserCredentialsImpl::getType,
                        Function.identity(),
                        (k1, k2) -> k2,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                    ));
    }

    @Override
    public Classifier getClassifier() {
        return this.info.getClassifier();
    }

    @Override
    public String getTopicName() {
        return info.getName();
    }

    @Override
    public String getBoostrapServers(String protocol) {
        return Optional.ofNullable(info.getAddresses().get(protocol))
                .map(values -> String.join(",", values))
                .orElse(null);
    }


    @Override
    public Optional<TopicUserCredentials> getCredentials(String type) {
        return Optional.ofNullable(credentials.get(type));
    }

    @Override
    public String getCACert() {
        return info.getCaCert();
    }

    @Override
    public Optional<Map<String, Object>> formatConnectionProperties() {
        return Extractor.extract(this);
    }

    @Override
    public boolean isVersioned() {
        return this.info.isVersioned();
    }

    @Override
    public int getNumPartitions() {
        return info.getActualSettings().getNumPartitions();
    }

    @Override
    public Map<String, String> getConfigs() {
        return info.getActualSettings().getConfigs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicAddressImpl that = (TopicAddressImpl) o;
        return info.equals(that.info);
    }

    @Override
    public int hashCode() {
        return info.hashCode();
    }
}
