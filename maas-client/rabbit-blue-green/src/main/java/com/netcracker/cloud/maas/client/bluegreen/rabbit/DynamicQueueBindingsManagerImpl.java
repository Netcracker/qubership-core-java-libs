package org.qubership.cloud.maas.client.bluegreen.rabbit;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DynamicQueueBindingsManagerImpl extends DynamicQueueBindingsManagerAbstract {
	public DynamicQueueBindingsManagerImpl(BlueGreenStatePublisher statePublisher) {
		super(statePublisher);
	}

	@Override
	protected void makeBinding(ChannelSupplier channelSupplier, String queue, String versionedExchange, String routingKey, Map<String, Object> arguments) {
		try (Channel channel = channelSupplier.get()) {
			log.info("Create binding for {} -> {}, routing key: {}", queue, versionedExchange, routingKey);
			channel.queueBind(queue, versionedExchange, routingKey, arguments);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Couldn't create binding, maybe versioned exchange doesn't exist for this version. queue '{}', versioned exchange '{}', error: {}", queue, versionedExchange, e);
		} catch (TimeoutException e) {
			log.error("Timeout exception, binding was not created. queue '{}', versioned exchange '{}', error: {}", queue, versionedExchange, e);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		}
	}

	@Override
	protected void removeBinding(ChannelSupplier channelSupplier, String queue, String versionedExchange, String routingKey, Map<String, Object> arguments) {
		try (Channel channel = channelSupplier.get()){
			log.info("Remove binding for {} -> {}, routing key: {}", queue, versionedExchange, routingKey);
			channel.queueUnbind(queue, versionedExchange, routingKey, arguments);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Couldn't unbind exchange, maybe versioned exchange doesn't exist for this version. queue '{}', versioned exchange '{}', error: {}", queue, versionedExchange, e);
		} catch (TimeoutException e) {
			log.error("Timeout exception, binding was not created. queue '{}', versioned exchange '{}', error: {}", queue, versionedExchange, e);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		}
	}
}
