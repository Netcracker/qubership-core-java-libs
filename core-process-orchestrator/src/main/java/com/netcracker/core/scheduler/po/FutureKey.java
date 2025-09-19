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
        if (obj instanceof String) {
            return taskId.equals(obj);
        } else if (obj instanceof UUID) {
            return uuid.equals(obj);
        } else if (obj instanceof FutureKey futureKey) {
            return taskId.equals(futureKey.taskId) && uuid.equals(futureKey.uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, uuid);
    }
}
