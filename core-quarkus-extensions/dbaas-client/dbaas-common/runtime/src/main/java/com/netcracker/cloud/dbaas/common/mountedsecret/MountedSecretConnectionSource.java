package com.netcracker.cloud.dbaas.common.mountedsecret;

import com.netcracker.cloud.dbaas.client.service.mountedsecret.MountedSecretSource;
import jakarta.inject.Singleton;

/**
 * CDI-visible bridge to dbaas-client-base's {@link MountedSecretSource}: the descriptor index,
 * throttled re-scan, eviction, and the synthetic-response {@code buildDatabase} all live there.
 * The per-driver {@code LogicalDbProvider}s inject this bean and use the inherited API directly.
 */
@Singleton
public class MountedSecretConnectionSource extends MountedSecretSource {

    public MountedSecretConnectionSource() {
    }

    public MountedSecretConnectionSource(String basePath) {
        super(basePath);
    }
}
