package org.qubership.cloud.maas.client.bluegreen.rabbit;

import java.util.Map;

public interface DynamicQueueBindingsManager {
    void queueBindDynamic(ChannelSupplier channelSupplier, String queue, String exchange, String routingKey, Map<String, Object> arguments);
    void queueUnbindDynamic(ChannelSupplier channelSupplier, String queue, String exchange, String routingKey, Map<String, Object> arguments);
}
