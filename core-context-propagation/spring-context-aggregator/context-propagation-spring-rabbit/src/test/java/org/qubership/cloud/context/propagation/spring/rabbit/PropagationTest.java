package org.qubership.cloud.context.propagation.spring.rabbit;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.spring.rabbit.annotation.EnableRabbitContextPropagation;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.headerstracking.filters.context.AllowedHeadersContext;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.qpid.server.SystemLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Headers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@EnableRabbit
@EnableRabbitContextPropagation
@SpringBootTest
public class PropagationTest {
	final static Logger log = LoggerFactory.getLogger(PropagationTest.class);
	final static SystemLauncher systemLauncher = new SystemLauncher();
	final static int port = getFreePort();
	final static URI cnnUri = URI.create("amqp://localhost:" + port);
	final static CountDownLatch awaitLatch = new CountDownLatch(1);

	private static final String CUSTOM_HEADER = "X-Custom-Header-1";
	private static final String CUSTOM_HEADER_VALUE = "case-insensitive-test-value";

	@Autowired
	RabbitTemplate template;

	@BeforeAll
	public static void setup() throws Exception {
		// start rabbitmq broker mock
		Map<String, Object> attributes = new HashMap<>();
		URL initialConfig = PropagationTest.class.getClassLoader().getResource("qpid-embedded-config.json");
		attributes.put("type", "Memory");
		attributes.put("context", Collections.singletonMap("qpid.amqp_port", port));
		attributes.put("initialConfigurationLocation", initialConfig.toExternalForm());
		attributes.put("startupLoggedToSystemOut", true);
		systemLauncher.startup(attributes);

		// create simple vhost configuration
		com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
		factory.setUri(cnnUri);
		try (Connection connection = factory.newConnection();
			 Channel channel = connection.createChannel()) {
			channel.exchangeDeclare("orders", "fanout");
			channel.queueDeclare("orders", true, false, false, null);
			channel.queueBind("orders", "orders", "invoice");
		}
		System.setProperty("headers.allowed", CUSTOM_HEADER.toLowerCase());
	}

    @AfterAll
    static void teardown() {
        System.clearProperty("headers.allowed");
    }

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	public void test() throws InterruptedException {
		AcceptLanguageContext.set("ZULU");
		AllowedHeadersContext.set(Map.of(CUSTOM_HEADER, CUSTOM_HEADER_VALUE));
		template.convertAndSend("orders", "invoice", "rye wheat");
		ContextManager.clearAll();

		if (!awaitLatch.await(10, TimeUnit.SECONDS)) {
			fail("Message listener failed or message doesn't even arrived in 10 seconds");
		}
	}


	@AfterAll
	public static void tearDown() {
		systemLauncher.shutdown();
	}

	@Configuration
	static class TestConfiguration {
		@Bean
		public ConnectionFactory connectionFactory() {
			return new CachingConnectionFactory(cnnUri);
		}

		@Bean
		public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
			return new RabbitTemplate(factory);
		}

		@Bean
		public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cnn) throws IOException {
			SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
			factory.setConnectionFactory(cnn);
			factory.setAfterReceivePostProcessors(message -> message);
			return factory;
		}

		@RabbitListener(queues = "orders")
		public void foo(String message, @Headers Map<String, Object> headers) {
			log.info("Message received: {}, lang: {}, headers: {}", message, AcceptLanguageContext.get(), headers);

			// test expected headers presence
			assertEquals("ZULU", headers.get(HttpHeaders.ACCEPT_LANGUAGE));

			// test restored context
			assertEquals("ZULU", AcceptLanguageContext.get());

			// test that custom header key is case-insensitive in restored context
			assertEquals(CUSTOM_HEADER_VALUE, AllowedHeadersContext.getHeaders().
					get(CUSTOM_HEADER.toLowerCase()));

			// finish test
			awaitLatch.countDown();
		}
	}

	static int getFreePort() {
		try(ServerSocket s = new ServerSocket(0)) {
			return s.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException("Error searching free port", e);
		}
	}
}
