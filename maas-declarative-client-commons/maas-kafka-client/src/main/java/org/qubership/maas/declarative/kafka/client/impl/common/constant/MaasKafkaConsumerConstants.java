package org.qubership.maas.declarative.kafka.client.impl.common.constant;

import java.util.List;

public interface MaasKafkaConsumerConstants {

    // time waiting values for waiting after exception occurred or sei duration for polling
    List<Long> DEFAULT_AWAIT_TIME_LIST = List.of(10000L, 20000L, 30000L, 60000L);
}
