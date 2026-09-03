package com.netcracker.cloud.maas.client.impl;

import com.netcracker.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import lombok.Getter;

import java.time.Duration;

public class ApiUrlProvider {
    @Getter
    private final ServerApiVersion serverApiVersion;
    private final String maasAgentUrl;

    public ApiUrlProvider(ServerApiVersion serverApiVersion, String maasAgentUrl) {
        this.serverApiVersion = serverApiVersion;
        this.maasAgentUrl = maasAgentUrl;
    }

    public String getKafkaTopicUrl(OnTopicExists onTopicExists) {
        String kafkaTopicUrl = getBaseUrl() + "/kafka/topic";
        if (onTopicExists != null) {
            kafkaTopicUrl += "?onTopicExists=" + onTopicExists.name().toLowerCase();
        }
        return kafkaTopicUrl;
    }

    public String getKafkaLazyTopicUrl() {
        return getBaseUrl() + "/kafka/lazy-topic";
    }

    public String getKafkaTopicTemplateUrl() {
        return getBaseUrl() + "/kafka/topic-template";
    }

    public String getKafkaTopicSearchUrl() {
        return getBaseUrl() + "/kafka/topic/search";
    }

    public String getKafkaTopicWatchCreateUrl(Duration timeout) {
        return String.format("%s/api/v2/kafka/topic/watch-create?timeout=%ds", maasAgentUrl, timeout.getSeconds());
    }

    public String getKafkaTopicGetByClassifierUrl() {
        return getBaseUrl() + "/kafka/topic/get-by-classifier";
    }

    public String getRabbitVhostUrl(boolean extended) {
        String rv = getBaseUrl() + "/rabbit/vhost";
        if (extended) {
            rv = rv + "?extended=true";
        }
        return rv;
    }

    public String getRabbitVhostGetByClassifierUrl(boolean extended) {
        String rv = getBaseUrl() + "/rabbit/vhost/get-by-classifier";
        if (extended) {
            rv = rv + "?extended=true";
        }
        return rv;
    }

    private String getBaseUrl() {
        // from 2.16 maas api version v1 is deprecated and must be avoided
        if (serverApiVersion.isCompatible(2, 16)) {
            return maasAgentUrl + "/api/v2";
        }
        return maasAgentUrl + "/api/v1";
    }
}
