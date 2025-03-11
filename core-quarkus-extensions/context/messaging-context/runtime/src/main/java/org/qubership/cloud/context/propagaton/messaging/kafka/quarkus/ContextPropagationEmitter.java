package org.qubership.cloud.context.propagaton.messaging.kafka.quarkus;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Decorator on standard smallrye {@link io.smallrye.reactive.messaging.providers.extension.EmitterImpl} with context
 * propagation feature.
 *
 * @param <T> message body type
 */
public class ContextPropagationEmitter<T> implements Emitter<T>{
	private final Logger log;
	private final Emitter<T> underlying;

	private Function<Metadata, Metadata> kafkaHeadersSupplier = m -> m;
	private Function<Metadata, Metadata> rabbitHeadersSupplier = m -> m;

	public ContextPropagationEmitter(String channelName, Emitter<T> underlying) {
		log = Logger.getLogger(ContextPropagationEmitter.class.getName() + ":" + channelName);
		this.underlying = underlying;

		try {
			Class.forName("io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata");
			log.debug("Add metadata enricher for rabbitmq messaging");
			kafkaHeadersSupplier = container -> {

				Optional<OutgoingKafkaRecordMetadata> kafkaMetadata = container.get(OutgoingKafkaRecordMetadata.class);
				Headers headers = new RecordHeaders();
				// add existing headers to new map
				kafkaMetadata.flatMap(meta -> Optional.ofNullable(meta.getHeaders()))
						.ifPresent(hrs -> hrs.forEach(headers::add));

				// dump context to headers
				RequestContextPropagation.populateResponse(
						new ContextDataCollector((key, value) -> {
							byte[] headerValue = Optional.ofNullable(value)
									.map(v -> v.toString().getBytes(StandardCharsets.UTF_8))
									.orElse(new byte[]{});
							headers.add(key, headerValue);
						})
				);

				// clone original with aggregated messages
				OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder nextBuillder = OutgoingKafkaRecordMetadata.builder();
				OutgoingKafkaRecordMetadata next = kafkaMetadata.map(original -> nextBuillder
							.withTopic(original.getTopic())
							.withKey(original.getKey())
							.withPartition(original.getPartition())
							.withTimestamp(original.getTimestamp())
					).orElse(nextBuillder)
					.withHeaders(headers)
					.build();

				return Metadata.of(next);
			};
		} catch (ClassNotFoundException|NoClassDefFoundError e) {
			log.info("Quarkus kafka messaging library is not presented in classpath");
		}

		try {
			Class.forName("io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata");
			log.debug("Add metadata enricher for rabbitmq messaging");
			rabbitHeadersSupplier = metadata -> {
				final OutgoingRabbitMQMetadata m = metadata.get(OutgoingRabbitMQMetadata.class)
						.orElseGet(() -> OutgoingRabbitMQMetadata.builder().build());

				Map<String, Object> headers = new HashMap<>();
				RequestContextPropagation.populateResponse(new ContextDataCollector(m.getHeaders()::put));
				return metadata.with(m);
			};
		} catch (ClassNotFoundException|NoClassDefFoundError e) {
			log.info("Quarkus rabbitmq messaging library is not presented in classpath");
		}
	}

	@Override
	public synchronized CompletionStage<Void> send(T payload) {
		Metadata meta = enrichMessageWithMetadata(payload);

		CompletableFuture<Void> future = new CompletableFuture();
		Message msg = ContextAwareMessage.of(payload)
				.withMetadata(ContextAwareMessage.captureContextMetadata(meta))
				.withAck(() -> {
					future.complete((Void) null);
					return CompletableFuture.completedFuture((Void) null);
				}).withNack((reason) -> {
					future.completeExceptionally(reason);
					return CompletableFuture.completedFuture((Void) null);
				});

		underlying.send(msg);
		return future;
	}


	@Override
	public synchronized <M extends Message<? extends T>> void send(M msg) {
		Metadata meta = enrichMessageWithMetadata(msg);
		underlying.send(msg.withMetadata(meta));
	}

	@Override
	public void complete() {
		underlying.complete();
	}

	@Override
	public void error(Exception e) {
		underlying.error(e);
	}

	@Override
	public boolean isCancelled() {
		return underlying.isCancelled();
	}

	@Override
	public boolean hasRequests() {
		return underlying.hasRequests();
	}

	static class ContextDataCollector implements OutgoingContextData {
		private final BiConsumer<String, Object> adaptor;

		public ContextDataCollector(BiConsumer<String, Object> adaptor) {
			this.adaptor = adaptor;
		}

		public void set(String s, Object o) {
			adaptor.accept(s, o);
		}
	}

	Metadata enrichMessageWithMetadata(Object payload) {
		Metadata meta;
		if (payload instanceof Message) {
			meta = ((Message)payload).getMetadata();
		} else {
			meta = Metadata.empty();
		}

		meta = kafkaHeadersSupplier.apply(meta);
		meta = rabbitHeadersSupplier.apply(meta);
		return meta;
	}
}
