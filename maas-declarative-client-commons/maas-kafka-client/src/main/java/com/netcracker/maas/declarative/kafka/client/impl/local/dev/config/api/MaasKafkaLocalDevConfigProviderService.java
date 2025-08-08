package org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api;

import java.util.Map;

/**
 * Used to provide properties under maas.kafka.local-dev.config prefix as Map
 */
public interface MaasKafkaLocalDevConfigProviderService {

    Map<String, Object> get();

}
