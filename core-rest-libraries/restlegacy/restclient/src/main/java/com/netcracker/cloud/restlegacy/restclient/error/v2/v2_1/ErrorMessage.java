package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

@Getter
@ToString
@RequiredArgsConstructor
public class ErrorMessage {

    private final String messageCode;
    private final Map<String, MessageParameter> parameters;

    public ErrorMessage(String messageCode) {
        this(messageCode, Collections.emptyMap());
    }
}
