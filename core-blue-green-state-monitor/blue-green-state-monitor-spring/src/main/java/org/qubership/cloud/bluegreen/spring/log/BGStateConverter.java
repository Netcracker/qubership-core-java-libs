package org.qubership.cloud.bluegreen.spring.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;

/**
 * A custom Logback converter that injects the Blue-Green state of current namespace into log messages.
 * <p>
 * This converter retrieves the current Blue-Green state
 * from a {@link BlueGreenStatePublisher} and makes it available in logging patterns.
 * If the state cannot be determined — for example, if the Spring application has not yet fully started —
 * it returns "-".
 * </p>
 *
 * <p>
 * The {@link BlueGreenStatePublisher} instance is provided via the static method {@link #setHolder(BlueGreenStatePublisher)}.
 * In a typical Spring Boot environment, this setup is handled automatically by
 * {@link BGStateConverterInitializer}.
 * </p>
 *
 * <p>Example usage in {@code logback.xml}:</p>
 * <pre>{@code
 * <conversionRule conversionWord="bg_state" converterClass="org.qubership.cloud.bluegreen.spring.log.BGStateConverter"/>
 *
 * <pattern>%d{HH:mm:ss.SSS} [%-5level] [bg_state:%bg_state] - %msg%n</pattern>
 * }</pre>
 */
public class BGStateConverter extends ClassicConverter {
    private static volatile BlueGreenStatePublisher blueGreenStatePublisher;

    static void setHolder(BlueGreenStatePublisher blueGreenStatePublisher) {
        BGStateConverter.blueGreenStatePublisher = blueGreenStatePublisher;
    }

    @Override
    public String convert(ILoggingEvent event) {
        if (blueGreenStatePublisher == null || blueGreenStatePublisher.getBlueGreenState() == null) {
            return "-";
        }
        return blueGreenStatePublisher.getBlueGreenState().getCurrent().getState().getName();
    }
}
