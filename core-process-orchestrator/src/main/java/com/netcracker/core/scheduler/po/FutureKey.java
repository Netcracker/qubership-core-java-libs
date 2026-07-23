package com.netcracker.core.scheduler.po;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FutureKey implements Serializable {

    @Getter
    private final String taskId;
    private final UUID uuid;

    public FutureKey(String taskId) {
        this.taskId = taskId;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FutureKey futureKey
                && taskId.equals(futureKey.taskId)
                && uuid.equals(futureKey.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, uuid);
    }
}
