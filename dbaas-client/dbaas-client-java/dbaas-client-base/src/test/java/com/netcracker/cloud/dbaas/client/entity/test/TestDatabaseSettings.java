package com.netcracker.cloud.dbaas.client.entity.test;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode
public class TestDatabaseSettings implements DatabaseSettings {
    private final UUID someUniqueSetting = UUID.randomUUID();
}
