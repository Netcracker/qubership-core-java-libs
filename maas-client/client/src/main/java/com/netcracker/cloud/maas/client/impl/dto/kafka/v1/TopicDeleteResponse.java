package com.netcracker.cloud.maas.client.impl.dto.kafka.v1;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TopicDeleteResponse {
    @JsonSetter(nulls = Nulls.SKIP)
    private List<TopicInfo> deletedSuccessfully = new ArrayList<>();
    @JsonSetter(nulls = Nulls.SKIP)
    private List<DeletionError> failedToDelete = new ArrayList<>();

    @Data
    public static class DeletionError {
        private String message;
        private TopicInfo topic;
    }
}
