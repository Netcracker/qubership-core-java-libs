package org.qubership.cloud.maas.bluegreen.kafka;

import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.util.Objects;

/**
 * Strategy to set initial kafka consumer offset position
 *
 * <p>For example, it may be either new candidate rollout or new service in rollout/update mode. So this service was not
 * presented in application before now and need setting telling kafka consumer about initial offset position need to set.
 * </p>
 */
@EqualsAndHashCode
public class OffsetSetupStrategy {
	/**
	 * Set consumer offset to the topic tail (ignore all existing messages in topic and start consume only new)
	 */
	public final static OffsetSetupStrategy LATEST = new OffsetSetupStrategy(Duration.ZERO, "LATEST");

	/**
	 * Set consumer offset to the start of topic (read messages from very beginning)
	 */
	public final static OffsetSetupStrategy EARLIEST = new OffsetSetupStrategy(Duration.ZERO, "EARLIEST");

	/**
	 * Rewind consumer offset to specified interval from topic end.
	 * @param rewindInterval into the past
	 * @return configuration instance
	 */
	public static OffsetSetupStrategy rewind(Duration rewindInterval) {
		return new OffsetSetupStrategy(rewindInterval, "rewind on " + rewindInterval);
	}

	private final Duration shift;
	private final String desc;
	private OffsetSetupStrategy(Duration rewindInterval, String desc) {
		this.shift = rewindInterval;
		this.desc = desc;
	}

	public Duration getShift() {
		return shift;
	}

	@Override
	public String toString() {
		return "OffsetSetupStrategy{" + desc + '}';
	}

	public static OffsetSetupStrategy valueOf(String value) {
		Objects.requireNonNull(value);

		if (LATEST.desc.equals(value)) {
			return LATEST;
		}
		if (EARLIEST.desc.equals(value)) {
			return EARLIEST;
		}
		return OffsetSetupStrategy.rewind(Duration.parse(value));
	}
}
