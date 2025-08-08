package com.netcracker.cloud.maas.client.context.rabbit;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionProvider;
import com.rabbitmq.client.AMQP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static com.netcracker.cloud.maas.client.context.rabbit.RabbitContextPropagation.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RabbitContextPropagationTest {
	@BeforeEach
	void setup() {
		ContextManager.clearAll();
	}

	@Test
	public void testRestore() {
		restoreContext(Collections.singletonMap(X_VERSION_SERIALIZATION_NAME, "v3"));
		assertEquals("v3", getVersionFromContext());
	}

	@Test
	public void testPropagateEmpty() {
		AMQP.BasicProperties props = propagateContext(new AMQP.BasicProperties());
		assertNull(props.getHeaders().get(VERSION_HEADER));
	}

	@Test
	public void testPropagatetoEmptyProps() {
		// setup context
		setVersionIntoContext("v3");

		// perform test
		AMQP.BasicProperties props = propagateContext(new AMQP.BasicProperties());
		assertEquals("v3", props.getHeaders().get(VERSION_HEADER));
	}

	@Test
	public void testPropagatetoNonEmptyProps() {
		// setup context
		setVersionIntoContext("v3");

		// perform test
		AMQP.BasicProperties  props = new AMQP.BasicProperties()
				.builder()
				.headers(Collections.singletonMap("abc", "cde"))
				.build();
		props = propagateContext(props);

		assertEquals("v3", props.getHeaders().get(VERSION_HEADER));
		assertEquals("cde", props.getHeaders().get("abc"));
	}

	@Test
	public void testRestoreEmpty() {
		restoreContext(Collections.EMPTY_MAP);
		assertEquals("", getVersionFromContext());
	}

	private void setVersionIntoContext(String version) {
		IncomingContextData ctxData = new MessageIncomingContextData(Collections.singletonMap(X_VERSION_SERIALIZATION_NAME, version));
		ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject(ctxData));
	}

	private String getVersionFromContext() {
		XVersionContextObject xVersionContextObject = ContextManager.get(XVersionProvider.CONTEXT_NAME);
		return xVersionContextObject.getXVersion();
	}
}
