package org.qubership.cloud.core.error.rest.exceptions;

import org.qubership.cloud.core.error.rest.tmf.model.Source;
import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import lombok.Getter;

public class DbaasAggregatorValidationException extends ErrorCodeException {
    @Getter
    Source source;

    public DbaasAggregatorValidationException(ErrorCode code, String message, Source source) {
        super(code, message);
        this.source = source;
    }
}
