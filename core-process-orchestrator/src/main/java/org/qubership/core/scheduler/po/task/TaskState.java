package org.qubership.core.scheduler.po.task;

import javax.annotation.Nullable;
import java.util.Arrays;

public enum TaskState {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In-Progress"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    TERMINATED("Terminated");

    final String name;

    TaskState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Nullable
    public static TaskState fromString(String value) {
        return Arrays.stream(values()).filter(t -> t.toString().equals(value)).findAny().orElse(null);
    }
}
