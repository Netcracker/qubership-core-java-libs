package org.qubership.cloud.context.propagation.messaging.kafka.quarkus;

import org.qubership.cloud.context.propagaton.messaging.kafka.quarkus.ContextAwareEmitterProducer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class BeanRegistrationsTest {
	private static final Logger log = LoggerFactory.getLogger(BeanRegistrationsTest.class);

	@Inject
	ContextAwareEmitterProducer producer;

	@Test
	public void testDumpedContext() {
		assertNotNull(producer);
	}
}
