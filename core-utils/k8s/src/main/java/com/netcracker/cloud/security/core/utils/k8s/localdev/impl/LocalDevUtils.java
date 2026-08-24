package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpResponse;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LocalDevUtils {

    /**
     * Reads a string field from a JSON node; returns {@code null} if missing, null, or blank.
     */
    static String getTextField(JsonNode node, String field) {
        String text = node.path(field).asText(null);
        return StringUtils.isBlank(text) ? null : text;
    }

    static String truncateResponseBody(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= LocalDevConstants.MAX_ERROR_BODY_LENGTH
                ? body
                : body.substring(0, LocalDevConstants.MAX_ERROR_BODY_LENGTH) + "...";
    }

    static boolean isFailed(int statusCode) {
        return statusCode / 100 != 2;
    }

    static void ensureSuccessful(HttpResponse<String> response, String operationDescription) {
        int status = response.statusCode();
        if (isFailed(status)) {
            throw new IllegalStateException(
                    operationDescription + " failed (HTTP " + status + "). Response: "
                            + truncateResponseBody(response.body()));
        }
    }
}
