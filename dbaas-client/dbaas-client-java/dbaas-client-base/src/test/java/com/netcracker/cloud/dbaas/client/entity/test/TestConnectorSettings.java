package com.netcracker.cloud.dbaas.client.entity.test;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.Discriminator;

public class TestConnectorSettings extends AbstractConnectorSettings {

    private final String customDiscriminator;

    public TestConnectorSettings(String customDiscriminator) {
        this.customDiscriminator = customDiscriminator;
    }

    @Override
    public Discriminator getDiscriminator() {
        return () -> customDiscriminator;
    }
}
