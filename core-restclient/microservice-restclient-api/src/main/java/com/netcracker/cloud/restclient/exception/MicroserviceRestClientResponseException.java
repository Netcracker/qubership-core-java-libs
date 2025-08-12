package com.netcracker.cloud.restclient.exception;


import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MicroserviceRestClientResponseException extends MicroserviceRestClientException {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceRestClientResponseException.class);
    @Getter
    private final int httpStatus;
    @Getter
    private final byte[] responseBody;
    @Getter
    private final Map<String, List<String>> responseHeaders;

    public MicroserviceRestClientResponseException(String msg,
                                                   int httpStatus,
                                                   byte[] responseBody,
                                                   Map<String, List<String>> responseHeaders) {
        super(msg);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
    }

    public MicroserviceRestClientResponseException(String msg,
                                                   Throwable cause,
                                                   int httpStatus,
                                                   byte[] responseBody,
                                                   Map<String, List<String>> responseHeaders) {
        super(msg, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
    }

    private Charset getResponseCharset() {
        Charset charset = StandardCharsets.ISO_8859_1;
        try {
            Optional<String> mediaTypeKey = responseHeaders.keySet()
                    .stream()
                    .filter("content-type"::equalsIgnoreCase)
                    .findFirst();
            if (mediaTypeKey.isPresent()) {
                List<String> mediaTypes = responseHeaders.getOrDefault(mediaTypeKey.get(), Collections.emptyList());
                String mediaType = mediaTypes.get(0);
                if (mediaType != null && !mediaType.isEmpty()) {
                    String[] parameters = mediaType.split(";");
                    for (String parameter : parameters) {
                        if (parameter != null && !parameter.isEmpty()) {
                            String[] split = parameter.split("=");
                            if (split.length == 2 && "charset".equalsIgnoreCase(split[0].trim())) {
                                charset = Charset.forName(split[1].trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get charset from response", e);
        }
        return charset;
    }

    public String getResponseBodyAsString() {
        Charset responseCharset = getResponseCharset();
        return new String(this.responseBody, responseCharset);
    }

    @Override
    public String toString() {
        return "MicroserviceRestClientResponseException{" +
                "message=" + getDetail() +
                ", httpStatus=" + httpStatus +
                ", responseBody=" + getResponseBodyAsString() +
                '}';
    }
}
