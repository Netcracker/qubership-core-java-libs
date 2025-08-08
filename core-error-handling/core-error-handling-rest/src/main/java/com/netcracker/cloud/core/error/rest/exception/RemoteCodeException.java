package org.qubership.cloud.core.error.rest.exception;

import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class RemoteCodeException extends ErrorCodeException {
    private Integer status;
    private Object source;
    private final Map<String, Object> meta = new TreeMap<>();

    public RemoteCodeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RemoteCodeException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
