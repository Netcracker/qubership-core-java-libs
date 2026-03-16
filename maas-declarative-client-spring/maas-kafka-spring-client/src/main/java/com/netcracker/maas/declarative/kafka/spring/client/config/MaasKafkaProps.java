package com.netcracker.maas.declarative.kafka.spring.client.config;

import com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class MaasKafkaProps {

    @Value("${"
            + MaasKafkaCommonConstants.ACCEPTABLE_TENANTS + ":"
            + "#{null}"
            + "}")
    List<String> acceptableTenants;

    @Value("${"
            + MaasKafkaCommonConstants.CONSUMER_THREAD_POOL_SIZE + ":"
            + MaasKafkaCommonConstants.CONSUMER_THREAD_POOL_SIZE_DEFAULT_VALUE
            + "}")
    Integer consumerThreadPoolSize;

    @Value("${"
            + MaasKafkaCommonConstants.CONSUMER_POOL_DURATION + ":"
            + MaasKafkaCommonConstants.CONSUMER_POOL_DURATION_DEFAULT_VALUE
            + "}")
    Integer consumerCommonPoolDuration;

    @Value("${"
            + MaasKafkaCommonConstants.MAAS_AGENT_URL + ":"
            + MaasKafkaCommonConstants.MAAS_AGENT_URL_DEFAULT_VALUE
            + "}")
    String maasAgentUrl;

    @Value("${"
            + MaasKafkaCommonConstants.CONSUMER_AWAIT_TIMEOUT_AFTER_ERROR + ":"
            + "#{null}"
            + "}")
    List<Long> awaitTimeoutAfterError;


    public List<String> getAcceptableTenants() {
        if (acceptableTenants == null) {
            return List.of();
        }
        // default value in case not provided value
        else if (acceptableTenants.size() == 1 && acceptableTenants.get(0).equals("")) {
            return List.of();
        }
        return acceptableTenants;
    }
}
