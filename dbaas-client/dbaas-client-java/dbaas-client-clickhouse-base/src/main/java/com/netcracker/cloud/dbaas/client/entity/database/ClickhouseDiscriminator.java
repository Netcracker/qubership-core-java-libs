package org.qubership.cloud.dbaas.client.entity.database;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;


@Builder
public class ClickhouseDiscriminator implements Discriminator {
    private String userRole;
    private String customDiscriminator;

    @Override
    public String getValue() {
        if (hasText(customDiscriminator)) {
            return customDiscriminator;
        }
        StringBuilder discriminator = new StringBuilder();
        if (hasText(userRole)) {
            discriminator.append(userRole).append(":");
        }
        if (!discriminator.isEmpty()) {
            return discriminator.deleteCharAt(discriminator.length() - 1).toString();
        }
        return null;
    }

    private boolean hasText(@Nullable String str) {
        return (str != null && !str.isEmpty());
    }

}
