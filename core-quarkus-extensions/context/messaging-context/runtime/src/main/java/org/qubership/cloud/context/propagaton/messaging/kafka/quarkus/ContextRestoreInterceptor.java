package org.qubership.cloud.context.propagaton.messaging.kafka.quarkus;

import org.qubership.cloud.maas.client.context.kafka.KafkaContextPropagation;
import io.smallrye.reactive.messaging.kafka.api.KafkaMessageMetadata;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

@Interceptor
@RestoreContext
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class ContextRestoreInterceptor implements Serializable {
	private static final Logger log = LoggerFactory.getLogger(ContextRestoreInterceptor.class);

	@AroundInvoke
	public Object restoreContext(InvocationContext inv) throws Exception {
		Message<?> messageParam = null;
		for (Object p : inv.getParameters()) {
			if (p instanceof Message) {
				messageParam = (Message<?>) p;
				break;
			}
		}

		if (messageParam == null) {
			throw new IllegalArgumentException("Cloud-Core context propagation framework for quarkus messaging only supports message listeners with Message<?> type as argument");
		}

		// search for metadata
		messageParam.getMetadata(KafkaMessageMetadata.class)
			.ifPresentOrElse(
					meta -> {
						if (log.isDebugEnabled()) {
							StringBuilder b = new StringBuilder();
							for (Header header : meta.getHeaders()) {
								if (b.length() > 0) {
									b.append(", ");
								}
								b.append(header.key())
										.append(":")
										.append(new String(header.value()));
							}
							log.debug("Restore context from: {}", b);
						}
						KafkaContextPropagation.restoreContext(meta.getHeaders());
					},
					() -> log.debug("No message metadata found to restore context")
			);

		return inv.proceed();
	}
}
