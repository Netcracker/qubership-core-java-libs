package org.qubership.cloud.core.error.rest.exceptions;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;
import lombok.Getter;

import java.util.UUID;

public class ProjectMsInternalException extends ErrorCodeException {
    // sample uuid filed to be included into meta
    @Getter
    private final UUID uuid;
    public ProjectMsInternalException(String message, UUID uuid, Throwable cause) {
        super(new ErrorCodeHolder("PROJECT-MS-6", "simple message"), message, cause);
        this.uuid = uuid;
    }
}
