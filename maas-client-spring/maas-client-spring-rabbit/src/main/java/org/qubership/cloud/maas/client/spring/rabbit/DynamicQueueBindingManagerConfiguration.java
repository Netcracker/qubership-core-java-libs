package org.qubership.cloud.maas.client.spring.rabbit;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.bluegreen.rabbit.DynamicQueueBindingsManager;
import org.qubership.cloud.maas.client.bluegreen.rabbit.DynamicQueueBindingsManagerImpl;
import org.qubership.cloud.maas.client.spring.rabbit.annotation.VersionedBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.Collection;
import java.util.LinkedList;

@Configuration
@Slf4j
public class DynamicQueueBindingManagerConfiguration {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ApplicationContext context;

    @Autowired
    BlueGreenStatePublisher publisher;

    @EventListener(ApplicationReadyEvent.class)
    public void onAppStartup() {
        final Collection<VersionedBinding> bindings = new LinkedList<>(context.getBeansOfType(VersionedBinding.class).values());
        log.info("Process versioned bindings: {}", bindings.toArray());

        final DynamicQueueBindingsManager manager = new DynamicQueueBindingsManagerImpl(publisher);
        rabbitTemplate.getConnectionFactory().addConnectionListener(connection -> {
            bindings.forEach(bgBinding -> {
                log.info("Creating versioned binding from exchange {} to queue {}", bgBinding.exchange, bgBinding.queue);
                manager.queueBindDynamic(() -> connection.createChannel(false), bgBinding.queue, bgBinding.exchange, bgBinding.routingKey, bgBinding.arguments);
            });
        });
    }
}
