package com.netcracker.cloud.bluegreen.spring.config;

import org.springframework.context.annotation.Import;

@Import(BlueGreenStatePublisherConfiguration.class)
public @interface EnableBlueGreenStatePublisher {
}
