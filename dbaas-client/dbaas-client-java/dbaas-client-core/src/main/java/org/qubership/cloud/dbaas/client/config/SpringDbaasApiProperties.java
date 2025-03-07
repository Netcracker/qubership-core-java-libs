package org.qubership.cloud.dbaas.client.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Getter
public class SpringDbaasApiProperties {
    private static final String DEFAULT_DBAAS_AGENT_URL = "http://dbaas-agent:8080";

    @Getter(AccessLevel.NONE)
    @Value("${dbaas.api.address:#{null}}")
    private Optional<String> address;

    @Value("${dbaas.api.retry.default.template.maxAttempts:10}")
    private int dbaasDefaultRetryMaxAttempts;

    @Value("${dbaas.api.retry.default.template.backOffPeriod.milliseconds:1000}")
    private int dbaasDefaultRetryBackOffPeriodInMs;

    @Value("${dbaas.api.retry.async.template.timeout.seconds:1200}")
    private int dbaasAsyncRetryTimeoutInS;

    public String getAddress() {
        return address.orElse(DEFAULT_DBAAS_AGENT_URL);
    }
}
