package org.qubership.cloud.bluegreen.impl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NSVersion {
    String name;
    String state;
    String version;
}
