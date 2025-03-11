package org.qubership.cloud.context.propagaton.messaging.kafka.quarkus;

import io.smallrye.reactive.messaging.providers.extension.ChannelProducer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;

@Dependent
public class ContextAwareEmitterProducer {
	private static final Logger log = Logger.getLogger(ContextAwareEmitterProducer.class);

	@Produces
	@Channel("")
	@Alternative
	@Priority(10)
	<T> Emitter<T> produceEmitter(ChannelProducer producer, InjectionPoint injectionPoint) {
		String channelName = injectionPoint.getQualifiers().stream()
				.filter(i -> i instanceof Channel)
				.findAny()
				.map(i -> ((Channel)i).value())
				.get();

		log.infof("Produce context aware emitter for: Channel('%s')", channelName);
		try {
			Method m = producer.getClass().getMethod("produceEmitter", new Class[]{InjectionPoint.class});
			Emitter<T> vanilla = (Emitter<T>) m.invoke(producer, injectionPoint);
			return new ContextPropagationEmitter(channelName, vanilla);
		} catch (Exception e) {
			throw new RuntimeException("Error create emitter delegate", e);
		}
	}
}
