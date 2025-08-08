package org.qubership.cloud.context.propagation.spring.kafka;

import jakarta.ws.rs.core.HttpHeaders;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.qubership.cloud.context.propagation.spring.kafka.annotation.EnableKafkaContextPropagation;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.headerstracking.filters.context.AllowedHeadersContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka // start embedded kafka instance
@EnableKafka // enable kafka to spring listener integration engine
@ComponentScan // to find Listener component
@EnableKafkaContextPropagation // inject configuration from testing library
public class ContextPropagationTest {
	private static final String TEST_LANG = "ZULU";
	private static final String CUSTOM_HEADER = "X-Custom-Header-1";
	private static final String CUSTOM_HEADER_VALUE = "case-insensitive-test-value";
	private static final CompletableFuture<ConsumerRecord<Integer, String>> consumed = new CompletableFuture<>();
	private static final CountingInterceptor interceptor = new CountingInterceptor();

	@Autowired
	private KafkaTemplate<Integer, String> producer;

	@BeforeAll
	static void setup() {
		System.setProperty("headers.allowed", CUSTOM_HEADER.toLowerCase());
	}

	@AfterAll
	static void tearDown() {
		System.clearProperty("headers.allowed");
	}

	@Test
	@Timeout(30)
	public void testContextPropagation() throws Exception {
		AcceptLanguageContext.set(TEST_LANG);
		AllowedHeadersContext.set(Map.of(CUSTOM_HEADER, CUSTOM_HEADER_VALUE));
		producer.send("orders", 1, "value" + 1);
		producer.flush();

		ConsumerRecord<Integer, String> message = consumed.get(5, TimeUnit.SECONDS);
		// additionally also check header, just in case
		assertEquals(TEST_LANG, new String(message.headers().lastHeader(HttpHeaders.ACCEPT_LANGUAGE).value()));
		assertEquals(CUSTOM_HEADER_VALUE, new String(message.headers().lastHeader(CUSTOM_HEADER).value()));
	}

	@Component
	public static class Listener {
		@KafkaListener(topics = "orders")
		public void listen(ConsumerRecord<Integer, String> message) {
			assertEquals(1, interceptor.countProcessed.get());

			Map<String, String> customHeaders = AllowedHeadersContext.getHeaders();
			System.out.println("Received: " + message + ", context language: " + AcceptLanguageContext.get());
			if (TEST_LANG.equals(AcceptLanguageContext.get()) &&
					CUSTOM_HEADER_VALUE.equals(customHeaders.get(CUSTOM_HEADER.toLowerCase()))) {
				consumed.complete(message);
			} else {
				consumed.completeExceptionally(new AssertionError("Test failed: Context not properly restored"));
			}
		}
	}

	@Configuration
	public static class TestsConfiguration {
		@Bean
		public ProducerFactory producerFactory(EmbeddedKafkaBroker kafkaEmbeded) {
			return new DefaultKafkaProducerFactory(KafkaTestUtils.producerProps(kafkaEmbeded));
		}

		@Bean
		public KafkaTemplate kafkaTemplate(ProducerFactory factory) {
			return new KafkaTemplate(factory);
		}

		@Bean
		public ConsumerFactory<Integer, String> consumerFactory(EmbeddedKafkaBroker kafkaEmbeded) {
			return new DefaultKafkaConsumerFactory(KafkaTestUtils.consumerProps(getClass().getName(), "true", kafkaEmbeded));
		}

		@Bean
		public KafkaListenerContainerFactory kafkaListenerContainerFactory(ConsumerFactory consumerFactory) {
			ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
			factory.setConsumerFactory(consumerFactory);
			factory.setConcurrency(1);
			factory.setRecordInterceptor(interceptor);
			return factory;
		}
	}

	static class CountingInterceptor implements RecordInterceptor<Integer, String> {
		public AtomicInteger countProcessed = new AtomicInteger();

		@Override
		public ConsumerRecord<Integer, String> intercept(ConsumerRecord<Integer, String> record, Consumer<Integer, String> consumer) {
			countProcessed.incrementAndGet();
			return record;
		}
	}
}

