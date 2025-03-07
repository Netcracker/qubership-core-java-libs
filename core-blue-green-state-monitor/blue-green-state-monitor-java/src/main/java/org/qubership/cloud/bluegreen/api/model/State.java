package org.qubership.cloud.bluegreen.api.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public enum State {
    IDLE("idle", "i"),
    ACTIVE("active", "a"),
    CANDIDATE("candidate", "c"),
    LEGACY("legacy", "l");

    private final String name;
    private final String shortName;

    State(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public static State fromShort(String shortName) {
        return Arrays.stream(values()).filter(s -> Objects.equals(s.shortName, shortName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown blue green status with short name: " + shortName));
    }
}
