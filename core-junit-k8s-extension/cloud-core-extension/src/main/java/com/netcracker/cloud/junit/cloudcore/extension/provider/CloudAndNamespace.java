package com.netcracker.cloud.junit.cloudcore.extension.provider;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CloudAndNamespace {
    private String cloud;
    private String namespace;
}
