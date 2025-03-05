package org.qubership.core.scheduler.po;

import java.io.Serializable;
import java.util.UUID;

public class FutureKey implements Serializable {

    private final String taskId;
    private final UUID uuid;

    public FutureKey(String taskId) {
        this.taskId = taskId;
        this.uuid = UUID.randomUUID();
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String) {
            return taskId.equals(obj);
        } else if (obj instanceof UUID) {
            return uuid.equals(obj);
        } else if (obj instanceof FutureKey) {
            return taskId.equals(((FutureKey) obj).taskId) && uuid.equals(((FutureKey) obj).uuid);
        }
        return false;
    }
}
