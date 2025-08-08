package org.qubership.cloud.dbaas.client.entity.database;

import org.jetbrains.annotations.Nullable;

public interface Discriminator {
    @Nullable
    String getValue();
}
