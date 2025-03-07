package org.qubership.cloud.bluegreen.api.error;

import org.qubership.cloud.bluegreen.api.model.LockAction;
import lombok.Getter;

import java.util.List;

@Getter
public class LockFailedException extends RuntimeException {
    public static final String ERROR_MESSAGE_TEMPLATE = "Failed to %s '%s' mutex in namespace(s): %s";
    private List<String> namespaces;
    private LockAction action;

    public LockFailedException(LockAction action, List<String> namespaces, String name, Throwable cause) {
        super(String.format(ERROR_MESSAGE_TEMPLATE, action.name().toLowerCase(), name, namespaces), cause);
        this.namespaces = namespaces;
        this.action = action;
    }

}
