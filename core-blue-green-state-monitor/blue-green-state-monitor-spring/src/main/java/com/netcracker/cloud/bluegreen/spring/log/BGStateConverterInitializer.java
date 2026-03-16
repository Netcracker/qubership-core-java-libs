package com.netcracker.cloud.bluegreen.spring.log;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;

/**
 * Initializes the {@link BGStateConverter} with a {@link BlueGreenStatePublisher} instance
 * once the Spring application context is fully refreshed.
 */
public class BGStateConverterInitializer {

    public BGStateConverterInitializer(BlueGreenStatePublisher blueGreenStatePublisher) {
        BGStateConverter.setHolder(blueGreenStatePublisher);
    }
}
