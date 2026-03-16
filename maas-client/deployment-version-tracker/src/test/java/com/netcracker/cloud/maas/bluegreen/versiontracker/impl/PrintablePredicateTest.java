package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrintablePredicateTest {
	@Test
	public void testPrint() {
		PrintablePredicate<Integer> pp = new PrintablePredicate<>(v -> v == 2, "v==2");
		assertTrue(pp.test(2));
		assertFalse(pp.test(1));
		assertEquals("v==2", pp.toString());
	}
}
