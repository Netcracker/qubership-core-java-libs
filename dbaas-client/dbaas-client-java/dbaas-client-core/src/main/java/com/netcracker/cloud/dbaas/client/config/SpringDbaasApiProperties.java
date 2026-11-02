package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Getter
public class SpringDbaasApiProperties {

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
     * @throws IllegalStateException if the M2M auth mode is k8s and {@code api.dbaas.address} is not set or empty
     */
    public String getAddress() {
        return DbaasApiAddress.resolve(M2MAuthMode.readFromEnv(), dbaasAgentAddress.orElse(null), dbaasAddress.orElse(null)).url();
    }
}
