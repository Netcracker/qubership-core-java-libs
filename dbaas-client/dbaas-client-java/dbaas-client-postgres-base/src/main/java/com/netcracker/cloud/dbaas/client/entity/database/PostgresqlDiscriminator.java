package com.netcracker.cloud.dbaas.client.entity.database;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;


@Builder
public class PostgresqlDiscriminator implements Discriminator {
    private String userRole;
    private String schema;
    private String customDiscriminator;
    private boolean roReplica;

    // userRole:schema
    @Override
    public String getValue() {
        if (hasText(customDiscriminator)) {
            return customDiscriminator;
        }
        StringBuilder discriminator = new StringBuilder();
        if (hasText(userRole)) {
            discriminator.append(userRole).append(":");
        }
        if (hasText(schema)) {
            discriminator.append(schema).append(":");
        }
        if (roReplica){
            discriminator.append("roReplica=true:");
        }
        else{
            discriminator.append("roReplica=false:");
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
