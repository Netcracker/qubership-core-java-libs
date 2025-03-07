package org.qubership.cloud.disableapi;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

import java.util.Set;

public class DeprecatedApiException extends ErrorCodeException {
    public DeprecatedApiException(String requestMethod, String requestUri, Set<String> matchingMethods, String matchingUri) {
        super(new ErrorCodeHolder("NC-COMMON-2101", "Request is declined with 404 Not Found, because deprecated REST API is disabled"),
                String.format("Request [%s] '%s' is declined with 404 Not Found, because the following deprecated REST API is disabled: [%s] %s",
                        requestMethod, requestUri, matchingMethods, matchingUri));
    }
}
