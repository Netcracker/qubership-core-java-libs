package com.netcracker.cloud.security.core.utils.k8s.localdev;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.http.HttpResponse;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LocalDevHttpUtils {

    static boolean isUnauthorized(int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }

    static boolean isFailed(int statusCode) {
        return statusCode / 100 != 2;
    }

    static void ensureSuccessful(HttpResponse<String> response, String operationDescription) {
        int status = response.statusCode();
        if (isUnauthorized(status)) {
            throw new IllegalStateException(
                    operationDescription + " unauthorized (HTTP " + status + "). Response: "
                            + LocalDevJsonUtils.truncateResponseBody(response.body()));
        }
        if (isFailed(status)) {
            throw new IllegalStateException(
                    operationDescription + " failed (HTTP " + status + "). Response: "
                            + LocalDevJsonUtils.truncateResponseBody(response.body()));
        }
    }
}
