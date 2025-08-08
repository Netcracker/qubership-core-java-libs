package org.qubership.cloud.maas.client.impl;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class LazyTest {
	@Test
	public void testNormal() {
		AtomicInteger runCounter = new AtomicInteger(0);
		Lazy lazy = new Lazy<>(() -> runCounter.incrementAndGet());
		assertEquals(0, runCounter.get());
		assertEquals(1, lazy.get());
		assertEquals(1, runCounter.get());
		assertEquals(1, lazy.get());
		assertEquals(1, runCounter.get());
	}

	@Test
	public void testNullSupplier() {
		AtomicInteger runCounter = new AtomicInteger(0);
		Lazy lazy = new Lazy<>(null);
		assertEquals(0, runCounter.get());
		assertThrows(IllegalArgumentException.class, () -> lazy.get());
	}

	@Test
	public void testExecutionException() {
		AtomicInteger runCounter = new AtomicInteger(0);
		Lazy lazy = new Lazy<>(() -> {
			runCounter.incrementAndGet();
			throw new RuntimeException("Oopsiki!");
		});

		assertThrows(RuntimeException.class, () -> lazy.get());
		assertThrows(RuntimeException.class, () -> lazy.get());
		assertEquals(2, runCounter.get());
	}

	@Test
	public void testMap() {
		AtomicInteger runCounter = new AtomicInteger(0);
		Lazy lazy = new Lazy<>(() -> runCounter.incrementAndGet());

		assertTrue(lazy.map(v -> fail("shouldn't be executed")).isEmpty());
		assertEquals(0, runCounter.get());

		// instantiate instance
		lazy.get();

		assertEquals(Optional.of("success"), lazy.map(v -> "success"));
		assertEquals(1, runCounter.get());
	}
}
