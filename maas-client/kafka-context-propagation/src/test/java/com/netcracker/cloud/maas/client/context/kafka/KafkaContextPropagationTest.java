package com.netcracker.cloud.maas.client.context.kafka;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider.ACCEPT_LANGUAGE;
import static org.qubership.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static org.qubership.cloud.maas.client.context.kafka.KafkaContextPropagation.*;
import static org.junit.jupiter.api.Assertions.*;

public class KafkaContextPropagationTest {
	@BeforeEach
	void setup() {
		ContextManager.clearAll();
	}

	@Test
	public void testRestore() {
		restoreContext(Collections.singletonList(new KafkaHeader(X_VERSION_SERIALIZATION_NAME, "v3".getBytes())));
		assertEquals("v3", getVersionFromContext());
	}

	@Test
	public void testRestoreEmpty() {
		restoreContext(Collections.emptyList());
		assertEquals("", getVersionFromContext());
	}

	@Test
	public void testAbstractPropagator() {
		setVersionIntoContext("v5");
		AcceptLanguageContext.set("ZULU");
		Map<String, Object> actual = new HashMap<>();
		dumpContext(actual::put);

		assertEquals("ZULU", actual.get(ACCEPT_LANGUAGE));
		assertEquals("v5", actual.get(X_VERSION_SERIALIZATION_NAME));
	}

	@Test
	public void testPropagate() {
		setVersionIntoContext("v3");
		List<Header> headers = propagateContext();

		boolean headerExists = false;
		for (Header header : headers) {
			if (header.key().equals(X_VERSION_SERIALIZATION_NAME)) {
				headerExists = true;
				assertEquals("v3", new String(header.value()));
			}
		}

		assertTrue(headerExists);
	}

	@Test
	public void testPropagateEmpty() {
		List<Header> headers = propagateContext();

		for (Header header : headers) {
			assertNotEquals("X-Version", header.key());
		}
	}



	private void setVersionIntoContext(String version) {
		IncomingContextData ctxData = new HeadersAdapter(Collections.singletonList(new KafkaHeader(X_VERSION_SERIALIZATION_NAME, version.getBytes())));
		ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject(ctxData));
	}

	private String getVersionFromContext() {
		XVersionContextObject xVersionContextObject = ContextManager.get(XVersionProvider.CONTEXT_NAME);
		return xVersionContextObject.getXVersion();
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
