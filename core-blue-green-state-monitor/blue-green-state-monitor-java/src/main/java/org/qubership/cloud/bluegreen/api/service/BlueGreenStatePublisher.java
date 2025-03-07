package org.qubership.cloud.bluegreen.api.service;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;

import java.util.function.Consumer;


/**
 * <p>
 * Allows to subscribe/unsubscribe for listening of {@link BlueGreenState} changes or get current {@link BlueGreenState}.
 * </p>
 */
public interface BlueGreenStatePublisher {

    /**
     * Subscribe to receive notifications about BlueGreenState changes
     *
     * @param subscriber {@link Consumer} which is invoked with initial BlueGreenState right after {@link #subscribe(Consumer)} method has been invoked
     *                   and is invoked each time BlueGreenState changes
     */
    void subscribe(Consumer<BlueGreenState> subscriber);

    /**
     * Unsubscribe to stop receiving notifications about BlueGreenState changes
     *
     * @param subscriber {@link Consumer}'s reference which was passed to {@link #subscribe(Consumer)} method
     */
    void unsubscribe(Consumer<BlueGreenState> subscriber);

    /**
     * @return Current BlueGreenState
     */
    BlueGreenState getBlueGreenState();

}
