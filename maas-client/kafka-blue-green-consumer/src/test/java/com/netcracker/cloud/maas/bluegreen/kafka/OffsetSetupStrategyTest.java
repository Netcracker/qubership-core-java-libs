package org.qubership.cloud.maas.bluegreen.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffsetSetupStrategyTest {
	@Test
	public void testAPI() {
		OffsetSetupStrategy coss = OffsetSetupStrategy.rewind(Duration.ofSeconds(5));
		assertEquals("OffsetSetupStrategy{rewind on PT5S}", coss.toString());
		assertEquals(Duration.ofSeconds(5), coss.getShift());
	}

	@Test
	public void testValueOf() {
		assertEquals(OffsetSetupStrategy.EARLIEST, OffsetSetupStrategy.valueOf("EARLIEST"));
		assertEquals(OffsetSetupStrategy.LATEST, OffsetSetupStrategy.valueOf("LATEST"));
		assertEquals(OffsetSetupStrategy.rewind(Duration.ofSeconds(15)), OffsetSetupStrategy.valueOf("PT15S"));
	}

}
