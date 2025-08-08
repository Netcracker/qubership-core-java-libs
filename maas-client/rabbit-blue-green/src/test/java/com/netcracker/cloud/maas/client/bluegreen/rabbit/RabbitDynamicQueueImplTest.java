package org.qubership.cloud.maas.client.bluegreen.rabbit;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.LocalDevBlueGreenStatePublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class RabbitDynamicQueueImplTest {

    @SneakyThrows
    @Test
    void testDynamicQueue() {
        ConnectionFactory factory = new MockConnectionFactory();

        var v1 = new Version("v1");
        var v2 = new Version("v2");
        var statePublisher = new LocalDevBlueGreenStatePublisher("test-namespace-1");

        try (final Connection conn = factory.newConnection()) {
            try (Channel channel = conn.createChannel()) {

                String exchangeName = "my-exchange";
                String queueName = "my-queue";

                //create exchange v1 to bind to it
                channel.exchangeDeclare(exchangeName+"-v1", "direct", true);
                channel.queueDeclare(queueName + "-" + statePublisher.getBlueGreenState().getCurrent().getVersion(), true, false, false, null);

                DynamicQueueBindingsManager dynamicQueue = new DynamicQueueBindingsManagerImpl(statePublisher);
                dynamicQueue.queueBindDynamic(() -> conn.createChannel(), queueName + "-" +statePublisher.getBlueGreenState().getCurrent().getVersion(), exchangeName, "", null);

                byte[] messageBodyBytes = "message-v1".getBytes();
                channel.basicPublish(exchangeName+"-v1", "", null, messageBodyBytes);

                GetResponse response = channel.basicGet(queueName + "-"+statePublisher.getBlueGreenState().getCurrent().getVersion(), false);
                if (response == null) {
                    fail("AMQP GetReponse must not be null for v1");
                } else {
                    byte[] body = response.getBody();
                    assertEquals(new String(body), "message-v1");
                    long deliveryTag = response.getEnvelope().getDeliveryTag();

                    channel.basicAck(deliveryTag, false);
                }

                //during warmup ms is copied and v2 should have been created, emulate it
                statePublisher.setBlueGreenState(new BlueGreenState(
                        new NamespaceVersion("test-namespace-1", State.CANDIDATE,  v2),
                        new NamespaceVersion("test-namespace-2", State.ACTIVE,  v1),
                        OffsetDateTime.now()
                ));

                channel.exchangeDeclare(exchangeName+"-"+statePublisher.getBlueGreenState().getCurrent().getVersion(), "direct", true);
                channel.queueDeclare(queueName + "-"+statePublisher.getBlueGreenState().getCurrent().getVersion(), true, false, false, null);
                dynamicQueue.queueBindDynamic(() -> conn.createChannel(), queueName + "-" +statePublisher.getBlueGreenState().getCurrent().getVersion(), exchangeName, "", null);

                messageBodyBytes = "message-v2".getBytes();
                channel.basicPublish(exchangeName+"-v2", "", null, messageBodyBytes);
                response = channel.basicGet(queueName +"-"+ statePublisher.getBlueGreenState().getCurrent().getVersion(), false);
                if (response == null) {
                    fail("AMQP GetReponse must not be null for v2");
                } else {
                    byte[] body = response.getBody();
                    assertEquals(new String(body), "message-v2");
                    long deliveryTag = response.getEnvelope().getDeliveryTag();

                    channel.basicAck(deliveryTag, false);
                }



                //unbind queue v1 and v2
                dynamicQueue.queueUnbindDynamic(() -> conn.createChannel(), queueName +"-" + statePublisher.getBlueGreenState().getSibling().get().getVersion(), exchangeName, "", null);

                messageBodyBytes = "message-v1-unbind".getBytes();
                channel.basicPublish(exchangeName+"-v1", "", null, messageBodyBytes);
                response = channel.basicGet(queueName + "-v1", false);
                if (response == null) {
                    fail("AMQP GetReponse must not be null for v1 after unbind v2");
                } else {
                    byte[] body = response.getBody();
                    assertEquals(new String(body), "message-v1-unbind");
                    long deliveryTag = response.getEnvelope().getDeliveryTag();

                    channel.basicAck(deliveryTag, false);
                }

                dynamicQueue.queueUnbindDynamic(() -> conn.createChannel(), queueName +"-" + statePublisher.getBlueGreenState().getCurrent().getVersion(), exchangeName, "", null);

                messageBodyBytes = "message-v2-unbind".getBytes();
                channel.basicPublish(exchangeName+"-v2", "", null, messageBodyBytes);
                response = channel.basicGet(queueName+"-v2", false);
                if (response != null) {
                    fail("AMQP GetReponse must be null for v2 after unbind");
                }
            }
        }
    }
}