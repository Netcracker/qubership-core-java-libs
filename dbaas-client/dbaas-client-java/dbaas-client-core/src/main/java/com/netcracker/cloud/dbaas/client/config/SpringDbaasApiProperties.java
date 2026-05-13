package com.netcracker.cloud.dbaas.client.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Slf4j
@Getter
public class SpringDbaasApiProperties {
    private static final String DEFAULT_DBAAS_AGENT_URL = "http://dbaas-agent:8080";

    @Getter(AccessLevel.NONE)
    @Value("${dbaas.api.address:#{null}}")
    private Optional<String> dbaasAgentAddress;

    @Getter(AccessLevel.NONE)
    @Value("${api.dbaas.address:#{null}}")
    private Optional<String> dbaasAddress;

    @Getter(AccessLevel.NONE)
    @Value("${security.m2m.kubernetes.enabled:false}")
    private boolean k8sEnabled;

    @Value("${dbaas.api.retry.default.template.maxAttempts:10}")
    private int dbaasDefaultRetryMaxAttempts;

    @Value("${dbaas.api.retry.default.template.backOffPeriod.milliseconds:1000}")
    private int dbaasDefaultRetryBackOffPeriodInMs;

    @Value("${dbaas.api.retry.async.template.timeout.seconds:1200}")
    private int dbaasAsyncRetryTimeoutInS;

    public String getAddress() {
        if(!k8sEnabled) {
            return dbaasAgentAddress.orElse(DEFAULT_DBAAS_AGENT_URL);
        }
        if(dbaasAddress.isEmpty()) {
            log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify 'api.dbaas.address' property to DBaaS url");
            return dbaasAgentAddress.orElse(DEFAULT_DBAAS_AGENT_URL);
        }
        return dbaasAddress.get();
    }
}
