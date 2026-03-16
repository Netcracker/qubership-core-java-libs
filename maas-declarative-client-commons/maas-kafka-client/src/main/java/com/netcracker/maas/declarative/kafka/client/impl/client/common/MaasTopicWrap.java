package com.netcracker.maas.declarative.kafka.client.impl.client.common;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class MaasTopicWrap {
    private TopicAddress topic;

    public MaasTopicWrap() {
    }

    public MaasTopicWrap(TopicAddress topic) {
        this.topic = topic;
    }

    public TopicAddress getTopic() {
        return topic;
    }

    public void setTopic(TopicAddress topic) {
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaasTopicWrap topicWrap = (MaasTopicWrap) o;
        return Objects.equals(topic, topicWrap.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("topic", topic)
                .toString();
    }
}
