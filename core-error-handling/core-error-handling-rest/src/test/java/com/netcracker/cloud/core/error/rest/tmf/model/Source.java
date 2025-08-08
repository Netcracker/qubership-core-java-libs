package org.qubership.cloud.core.error.rest.tmf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {
    private String pointer;
    private String parameter;
}
