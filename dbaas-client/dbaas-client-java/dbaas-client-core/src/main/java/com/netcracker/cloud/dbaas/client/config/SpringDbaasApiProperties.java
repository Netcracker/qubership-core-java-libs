package com.netcracker.cloud.dbaas.client.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Getter
public class SpringDbaasApiProperties {
    @Value("${api.dbaas.address:#{null}}")
    private Optional<String> address;

    @Value("${dbaas.api.retry.default.template.maxAttempts:10}")
    private int dbaasDefaultRetryMaxAttempts;

    @Value("${dbaas.api.retry.default.template.backOffPeriod.milliseconds:1000}")
    private int dbaasDefaultRetryBackOffPeriodInMs;

    @Value("${dbaas.api.retry.async.template.timeout.seconds:1200}")
    private int dbaasAsyncRetryTimeoutInS;
}
