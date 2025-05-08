package org.qubership.cloud.context.propagation.spring.rabbit;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Context propagation class with RabbitMQ specific: add alias for x-version context to properly support
 * platform blue/green feature
 */
public class RabbitContextPropagation {
	static final String VERSION_HEADER = "version";

	/**
	 * Dump current context values to headers map
	 * @param props headers merge context to
	 * @return context in AMQP.BasicProperties headers
	 */
	public static AMQP.BasicProperties propagateContext(AMQP.BasicProperties props) {
		HashMap<String, Object> headers = new HashMap<>();
		if (props != null && props.getHeaders() != null) {
			headers.putAll(props.getHeaders());
		}

		dumpContext(headers::put);
		return props.builder().headers(headers).build();
	}

	/**
	 * Generic method to dump context pairs to arbitrary data structure. Can be used to context propagation as well
	 * @param adaptor
	 */
	public static void dumpContext(BiConsumer<String, Object> adaptor) {
		AbstractContextDataCollector bridge = new AbstractContextDataCollector(adaptor);
		RequestContextPropagation.populateResponse(bridge);
	}

	/**
	 * Reconstruct context from message headers into current thread
	 * @param delivery rabbitmq message
	 */
	public static void restoreContext(Delivery delivery) {
		restoreContext(delivery.getProperties().getHeaders());
	}

	/**
	 * Reconstruct context from message headers into current thread
	 * @param headers mpa of message headers
	 */
	public static void restoreContext(Map<String, Object> headers) {
		RequestContextPropagation.clear();
		RequestContextPropagation.initRequestContext(new MessageIncomingContextData(headers));
	}

	static class AbstractContextDataCollector implements OutgoingContextData {
		private final BiConsumer<String, Object> adaptor;

		public AbstractContextDataCollector(BiConsumer<String, Object> adaptor) {
			this.adaptor = adaptor;
		}

		public void set(String s, Object o) {
			adaptor.accept(s, o);

			// in order to support blue/green deployment through messaging, we have to add `version' header as alias
			// to unprocessable in rabbitmq bindings `x-version' (`x-' prefixed headers in not permitted for match
			// evaluations https://www.rabbitmq.com/tutorials/amqp-concepts.html#exchange-headers)
			if ("x-version".equalsIgnoreCase(s)) {
				adaptor.accept(VERSION_HEADER, o);
			}
		}
	}

	static class MessageIncomingContextData implements IncomingContextData {
		final Map<String, Object> headers;

		public MessageIncomingContextData(Map<String, Object> src) {
			this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			this.headers.putAll(src);
		}

		@Override
		public Object get(String s) {
			return convert(headers.get(s));
		}

		@Override
		public Map<String, List<?>> getAll() {
			Map<String, List<?>> result = new HashMap<>();
			for(Map.Entry<String, Object> e : headers.entrySet()) {
				result.put(e.getKey(), Collections.singletonList(convert(e.getValue())));
			}

			return result;
		}

		Object convert(Object value) {
			return  value != null ? value.toString() : null;
		}
	}
}
