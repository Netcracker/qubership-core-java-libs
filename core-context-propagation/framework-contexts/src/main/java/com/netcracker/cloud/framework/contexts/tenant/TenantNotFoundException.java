package org.qubership.cloud.framework.contexts.tenant;

import org.jetbrains.annotations.NotNull;

/**
 * This exception should be throws if tenant-id was not found in jwt token or http header
 */
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(@NotNull String message) {
        super(message);
    }
}
