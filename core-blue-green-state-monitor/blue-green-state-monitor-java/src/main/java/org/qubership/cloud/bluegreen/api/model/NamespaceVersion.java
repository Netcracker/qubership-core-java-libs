package org.qubership.cloud.bluegreen.api.model;


import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class NamespaceVersion {
    String namespace;
    State state;
    Version version;
}
