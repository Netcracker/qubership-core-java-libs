package org.qubership.cloud.context.propagation.spring.kafka;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public class KafkaContextPropagation {

	/**
	 * Dump current context as list of headers
	 * @return context in list headers
	 */
	public static List<Header> propagateContext() {
		List<Header> headers = new ArrayList<>();
		RequestContextPropagation.populateResponse(
				new AbstractContextDataCollector((k, v) ->
						headers.add(new KafkaHeader(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8)))
				)
		);
		return headers;
	}

	/**
	 * Generic method to dump context pairs to arbitrary data structure. Can be used to context propagation as well
	 * @param adaptor
	 */
	public static void dumpContext(BiConsumer<String, Object> adaptor) {
		AbstractContextDataCollector bridge = new AbstractContextDataCollector(adaptor);
		RequestContextPropagation.populateResponse(bridge);
	}

	public static void restoreContext(Iterable<Header> headers) {
		RequestContextPropagation.clear();
		RequestContextPropagation.initRequestContext(new HeadersAdapter(headers));
	}

	static class AbstractContextDataCollector implements OutgoingContextData {
		private final BiConsumer<String, Object> adaptor;

		public AbstractContextDataCollector(BiConsumer<String, Object> adaptor) {
			this.adaptor = adaptor;
		}

		public void set(String s, Object o) {
			adaptor.accept(s, o);
		}
	}

	static class HeadersAdapter implements IncomingContextData {
		final Map<String, Object> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		public HeadersAdapter(Iterable<Header> headers) {
			for(Header header : headers) {
				this.headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
			}
		}

		@Override
		public Object get(String s) {
			return headers.get(s);
		}

		@Override
		public Map<String, List<?>> getAll() {
			Map<String, List<?>> result = new HashMap<>();
			for(Map.Entry<String, Object> e : headers.entrySet()) {
				result.put(e.getKey(), Collections.singletonList(e.getValue()));
			}

			return result;
		}
	}

	private static class KafkaHeader implements Header {
		private final String key;
		private final byte[] value;

		KafkaHeader(String key, byte[] value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public byte[] value() {
			return value;
		}
	}


}
