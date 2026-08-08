package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LocalDevJsonUtils {

    /**
     * Reads a string field from a JSON node; returns {@code null} if missing, null, or blank.
     */
    static String getTextField(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return StringUtils.isBlank(text) ? null : text;
    }

    static String firstNonBlank(String first, String second) {
        if (StringUtils.isNotBlank(first)) {
            return first;
        }
        if (StringUtils.isNotBlank(second)) {
            return second;
        }
        return null;
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
}
