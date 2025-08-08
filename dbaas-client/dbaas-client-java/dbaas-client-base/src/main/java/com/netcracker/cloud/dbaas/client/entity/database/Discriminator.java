package com.netcracker.cloud.dbaas.client.entity.database;

import org.jetbrains.annotations.Nullable;

public interface Discriminator {
    @Nullable
    String getValue();
}
