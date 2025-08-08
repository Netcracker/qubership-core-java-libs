package com.netcracker.cloud.maas.client.spring.rabbit;

import com.github.fridujo.rabbitmq.mock.compatibility.MockConnectionFactoryFactory;
import org.qubership.cloud.maas.client.spring.rabbit.annotation.EnableDynamicQueueBindings;
import org.qubership.cloud.maas.client.spring.rabbit.annotation.VersionedBinding;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest()
@EnableRabbit
@EnableDynamicQueueBindings
@TestPropertySource("classpath:application-test.yaml")
class RabbitDynamicQueueImplSpringTest {

    @Autowired
    Queue queue;
    @Autowired
    Exchange exchange;
    @Autowired
    RabbitTemplate template;

    @Test
    @SneakyThrows
    void testDynamicQueue() {
        template.convertAndSend(exchange.getName(), "", "abc");
        Message message = template.receive(queue.getName());
        assertEquals("abc", new String(message.getBody()));
    }

    @Configuration
    public static class TestsConfiguration {
        // prerequisites =======================================================================
        @Bean
        public ConnectionFactory connectionFactory() {
            return new CachingConnectionFactory(MockConnectionFactoryFactory.build());
        }

        @Bean
        public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
            return new RabbitAdmin(connectionFactory);
        }

        @Bean
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
            return new RabbitTemplate(connectionFactory);
        }

        // emulate maas defined structure for versioned exchanges (create only one versioned exchange)
        @Bean
        Exchange versionedExchange() {
            return new TopicExchange("my-exchange-v1");
        }

        // User defined beans ==================================================================
        @Bean
        Queue myQueue() {
            return new Queue("my-queue");
        }

        @Bean
        VersionedBinding myBinding() {
            return new VersionedBinding("my-exchange", "my-queue", "", null);
        }
    }
}
