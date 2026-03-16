package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
@Getter
public class FieldValidationMessage {
    private final String fieldName;
    private final ErrorMessage validationMessage;
}
