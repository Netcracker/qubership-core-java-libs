package org.qubership.cloud.core.error.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorCodeHolder implements ErrorCode {
    private final String code;
    private final String title;
}
