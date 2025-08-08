package com.netcracker.cloud.dbaas.client.cassandra.migration.service.await.ak;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.operation.TableOperationType;

import java.util.Arrays;

enum ExpectedTableStatus {
    ACTIVE, NOT_EXISTS;

    public static boolean isPresent(String value) {
        return Arrays.stream(ExpectedTableStatus.values())
                .anyMatch(enumValue -> enumValue.name().equalsIgnoreCase(value));
    }

    public static ExpectedTableStatus resolve(TableOperationType tableOperationType) {
        switch (tableOperationType) {
            case CREATE, UPDATE -> {
                return ACTIVE;
            }
            case DROP -> {
                return NOT_EXISTS;
            }
        }
        throw new IllegalArgumentException("Unable to resolve expected status for table operation " + tableOperationType);
    }
}
