package com.netcracker.maas.declarative.kafka.client.impl.definition.api;

import java.util.Map;

public interface MaasKafkaClientConfigPlatformService {

    Map<String, Object> getClientConfigByPrefix(String prefix);
}
