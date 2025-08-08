package com.netcracker.cloud.maas.client.api.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchCriteria {
    String topic;
    String namespace;
    String instance;
}
