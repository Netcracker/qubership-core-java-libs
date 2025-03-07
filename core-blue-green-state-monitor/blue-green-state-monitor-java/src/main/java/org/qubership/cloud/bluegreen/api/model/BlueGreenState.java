package org.qubership.cloud.bluegreen.api.model;

import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Value
public class BlueGreenState {
    NamespaceVersion current;
    Optional<NamespaceVersion> sibling;
    OffsetDateTime updateTime;

    public BlueGreenState(NamespaceVersion current, OffsetDateTime updateTime) {
        this(current, Optional.empty(), updateTime);
    }

    public BlueGreenState(NamespaceVersion current, NamespaceVersion sibling, OffsetDateTime updateTime) {
        this(current, Optional.ofNullable(sibling), updateTime);
    }

    public BlueGreenState(NamespaceVersion current, Optional<NamespaceVersion> sibling, OffsetDateTime updateTime) {
        Objects.requireNonNull(current, "current NamespaceVersion cannot be null");
        Objects.requireNonNull(sibling, "sibling NamespaceVersion cannot be null");
        Objects.requireNonNull(sibling, "updateTime cannot be null");
        this.current = current;
        this.sibling = sibling;
        this.updateTime = updateTime;
    }
}
