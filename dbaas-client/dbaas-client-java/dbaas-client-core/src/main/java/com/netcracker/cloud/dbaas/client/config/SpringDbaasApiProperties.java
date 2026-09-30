package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
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

    @Value("${dbaas.api.retry.default.template.maxAttempts:10}")
    private int dbaasDefaultRetryMaxAttempts;

    @Value("${dbaas.api.retry.default.template.backOffPeriod.milliseconds:1000}")
    private int dbaasDefaultRetryBackOffPeriodInMs;

    @Value("${dbaas.api.retry.async.template.timeout.seconds:1200}")
    private int dbaasAsyncRetryTimeoutInS;

    /**
     * @throws IllegalStateException if the M2M auth mode is k8s and {@code api.dbaas.address} is not set
     */
    public String getAddress() {
        return switch (M2MAuthMode.read()) {
            case LEGACY -> agentAddress();
            case HYBRID -> dbaasAddress.orElseGet(() -> {
                log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify 'api.dbaas.address' property to DBaaS url");
                return agentAddress();
            });
            case K8S -> dbaasAddress.orElseThrow(() -> new IllegalStateException(
                    "api.dbaas.address is not set: with M2M_AUTH_MODE=k8s the client sends requests directly to DBaaS, set api.dbaas.address to the DBaaS URL"));
        };
    }

    private String agentAddress() {
        return dbaasAgentAddress.orElse(DEFAULT_DBAAS_AGENT_URL);
    }
}
