package org.qubership.cloud.maas.client.bluegreen.rabbit;

import com.rabbitmq.client.Channel;

@FunctionalInterface
public interface ChannelSupplier {
    Channel get() throws Exception;
}
