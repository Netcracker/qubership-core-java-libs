package com.netcracker.cloud.dbaas.client.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;


public class DefaultMSInfoProvider implements MSInfoProvider {

    @Getter
    @Value("${cloud.microservice.name}")
    private String microserviceName;

    @Getter
    @Value("${cloud.microservice.namespace}")
    private String namespace;

    @Getter
    @Value("${dbaas.core.localdev:#{null}}")
    private String localDevNamespace;
}
