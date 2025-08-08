package com.netcracker.cloud.maas.client.impl.rabbit;

import org.qubership.cloud.bluegreen.impl.service.LocalDevBlueGreenStatePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class RabbitHelperMethodsTest {
	RabbitMaaSClientImpl client;

	@BeforeEach
	public void setup() {
		client = new RabbitMaaSClientImpl( null, null);
	}

	@Test
	public void testVersionedQueueName1() {
		assertEquals("abc-v1", client.getVersionedQueueName("abc", new LocalDevBlueGreenStatePublisher("test-namespace")));
	}

	@Test
	public void testGetExchangeNames() {
		assertIterableEquals(Collections.singleton("abc"), new RabbitMaaSClientImpl( null, null).getExchangeNames("abc"));
	}
}
