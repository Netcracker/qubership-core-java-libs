package org.qubership.cloud.bluegreen.api.model;


import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@AllArgsConstructor
public class MicroserviceLockInfo {
    String namespace;
    String microserviceName;
    String podName;
    String name;
    String reason;
    OffsetDateTime timestamp;
}
