package org.qubership.cloud.maas.client.bluegreen.rabbit;

import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public abstract class DynamicQueueBindingsManagerAbstract implements DynamicQueueBindingsManager {
    private final BlueGreenStatePublisher statePublisher;

    protected DynamicQueueBindingsManagerAbstract(BlueGreenStatePublisher statePublisher) {
        this.statePublisher = statePublisher;
    }

    @Override
    public void queueBindDynamic(ChannelSupplier channelSupplier, String queue, String exchange, String routingKey, Map<String, Object> arguments) {
        String versionedExchange = getVersionedExchangeName(exchange);
        log.info("Binding dynamic queue with versioned exchange: {}", versionedExchange);

        //in bg v2 we have a copy of ms and its queue during warmup and we don't need to make binding to active version
        makeBinding(channelSupplier, queue, versionedExchange, routingKey, arguments);
    }

    protected abstract void makeBinding(ChannelSupplier channelSupplier, String queue, String versionedExchange, String routingKey, Map<String, Object> arguments);
    protected abstract void removeBinding(ChannelSupplier channelSupplier, String queue, String versionedExchange, String routingKey, Map<String, Object> arguments);

    @Override
    public void queueUnbindDynamic(ChannelSupplier channelSupplier, String queue, String exchange, String routingKey, Map<String, Object> arguments) {
        String versionedExchange = getVersionedExchangeName(exchange);
        removeBinding(channelSupplier, queue, versionedExchange, routingKey, arguments);
    }

    private String getVersionedExchangeName(String exchangeName) {
        return getVersionedExchangeName(exchangeName, statePublisher.getBlueGreenState().getCurrent().getVersion());
    }

    private String getVersionedExchangeName(String exchangeName, Version version) {
        return exchangeName + "-" + version.value();
    }
}
