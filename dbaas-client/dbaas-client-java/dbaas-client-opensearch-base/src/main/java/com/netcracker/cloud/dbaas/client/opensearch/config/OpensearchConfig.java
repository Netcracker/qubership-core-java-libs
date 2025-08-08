package com.netcracker.cloud.dbaas.client.opensearch.config;

import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OpensearchConfig {
    private OpensearchProperties opensearchProperties;
    private String delimiter;
}
