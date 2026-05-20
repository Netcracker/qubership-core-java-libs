package com.netcracker.cloud.context.propagation.spring.rabbit;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.spring.rabbit.annotation.EnableRabbitContextPropagation;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;
import com.netcracker.cloud.headerstracking.filters.context.AcceptLanguageContext;
import com.netcracker.cloud.headerstracking.filters.context.AllowedHeadersContext;
import com.netcracker.cloud.headerstracking.filters.context.ChannelRequestIdContext;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.qpid.server.SystemLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@EnableRabbit
@EnableRabbitContextPropagation
@SpringBootTest
public class PropagationTest {
	static final Logger log = LoggerFactory.getLogger(PropagationTest.class);
	static final SystemLauncher systemLauncher = new SystemLauncher();
	static final int port = getFreePort();
	static final URI cnnUri = URI.create("amqp://localhost:" + port);
	static final AtomicReference<CountDownLatch> awaitLatch = new AtomicReference<>();
	static final AtomicReference<Map<String, Object>> receivedHeaders = new AtomicReference<>();

	private static final String CUSTOM_HEADER = "X-Custom-Header-1";
	private static final String CUSTOM_HEADER_VALUE = "case-insensitive-test-value";
	private static final String ANOTHER_HEADER = "X-Custom-Header-2";
	private static final String ANOTHER_HEADER_VALUE = "blocked-value";
	private static final String X_CHANNEL_REQUEST_ID_NAME = "X-Channel-Request-Id";
	private static final String X_CHANNEL_REQUEST_ID_VALUE = "456";

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
		System.clearProperty("context.propagation.headers.enable.optional");
	}

    @AfterAll
    static void teardown() {
        System.clearProperty("headers.allowed");
    }

    @BeforeEach
    void beforeEach() {
        awaitLatch.set(new CountDownLatch(1));
        receivedHeaders.set(null);
    }

    @AfterEach
    void afterEach() {
        System.clearProperty("context.propagation.headers.enable.optional");
		HeaderPropagationConfiguration.resetCache();
    }

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	void testXChannelRequestIdBlockedByDefault() throws InterruptedException {
		AcceptLanguageContext.set("ZULU");
		AllowedHeadersContext.set(Map.of(CUSTOM_HEADER, CUSTOM_HEADER_VALUE));
		ChannelRequestIdContext.set(X_CHANNEL_REQUEST_ID_VALUE);
		template.convertAndSend("orders", "invoice", "rye wheat");
		ContextManager.clearAll();

		if (!awaitLatch.get().await(10, TimeUnit.SECONDS)) {
			fail("Message listener failed or message doesn't even arrived in 10 seconds");
		}

        assertNull(getHeaderIgnoreCase(receivedHeaders.get(), X_CHANNEL_REQUEST_ID_NAME));
	}

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testXChannelRequestIdAllowedWhenExempted() throws InterruptedException {
        System.setProperty("context.propagation.headers.enable.optional", X_CHANNEL_REQUEST_ID_NAME);
        HeaderPropagationConfiguration.resetCache();

        AcceptLanguageContext.set("ZULU");
        AllowedHeadersContext.set(Map.of(CUSTOM_HEADER, CUSTOM_HEADER_VALUE));
        ChannelRequestIdContext.set(X_CHANNEL_REQUEST_ID_VALUE);
        template.convertAndSend("orders", "invoice", "rye wheat");
        ContextManager.clearAll();

        if (!awaitLatch.get().await(10, TimeUnit.SECONDS)) {
            fail("Message listener failed or message doesn't even arrived in 10 seconds");
        }

        assertEquals(X_CHANNEL_REQUEST_ID_VALUE, getHeaderIgnoreCase(receivedHeaders.get(), X_CHANNEL_REQUEST_ID_NAME));
    }

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	void testUnknownExemptionDoesNotAffectRestrictedList() throws InterruptedException {
		System.setProperty("context.propagation.headers.enable.optional", ANOTHER_HEADER);
		HeaderPropagationConfiguration.resetCache();

		AcceptLanguageContext.set("ZULU");
		AllowedHeadersContext.set(Map.of(
				CUSTOM_HEADER, CUSTOM_HEADER_VALUE,
				ANOTHER_HEADER, ANOTHER_HEADER_VALUE));
		ChannelRequestIdContext.set(X_CHANNEL_REQUEST_ID_VALUE);
		template.convertAndSend("orders", "invoice", "rye wheat");
		ContextManager.clearAll();

		if (!awaitLatch.get().await(10, TimeUnit.SECONDS)) {
			fail("Message listener failed or message doesn't even arrived in 10 seconds");
		}

		assertNull(getHeaderIgnoreCase(receivedHeaders.get(), X_CHANNEL_REQUEST_ID_NAME),
				"Restricted list must remain intact when no entry matches it");
		assertEquals(CUSTOM_HEADER_VALUE, getHeaderIgnoreCase(receivedHeaders.get(), CUSTOM_HEADER));
		assertEquals(ANOTHER_HEADER_VALUE, getHeaderIgnoreCase(receivedHeaders.get(), ANOTHER_HEADER));
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
			receivedHeaders.set(headers);
			awaitLatch.get().countDown();
		}
	}

	private static Object getHeaderIgnoreCase(Map<String, Object> headers, String name) {
		return headers.entrySet().stream()
				.filter(entry -> entry.getKey().equalsIgnoreCase(name))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
	}

	static int getFreePort() {
		try(ServerSocket s = new ServerSocket(0)) {
			return s.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException("Error searching free port", e);
		}
	}
}
