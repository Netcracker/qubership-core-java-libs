package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpResponse;
import java.util.Base64;

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

    /**
     * Pads a Base64URL JWT segment so {@link Base64#getUrlDecoder()} accepts it.
     * JWT payloads use Base64URL without padding; Java's decoder requires length % 4 == 0.
     */
    static String padBase64Url(String base64Url) {
        int remainder = base64Url.length() % LocalDevConstants.JWT_BASE64_PAD_LENGTH;
        if (remainder == 0) {
            return base64Url;
        }
        return base64Url + "====".substring(remainder);
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
