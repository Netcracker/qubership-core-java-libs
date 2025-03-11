package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

@Value
@EqualsAndHashCode
public class DatabaseKey {
    @NotNull
    private Map<String, Object> classifier;
    private String discriminator;
}
