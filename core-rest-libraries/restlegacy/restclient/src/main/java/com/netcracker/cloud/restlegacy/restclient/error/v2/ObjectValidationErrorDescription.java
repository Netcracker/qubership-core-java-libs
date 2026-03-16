package com.netcracker.cloud.restlegacy.restclient.error.v2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.annotation.Nullable;
import java.util.List;

@ToString
@RequiredArgsConstructor
public class ObjectValidationErrorDescription implements HasDebugInfo<DebugInfo> {

    @Setter
    @Getter
    private String objectValidationMessage;

    @Getter
    private final List<FieldValidationErrorDescription> validationErrors;

    @Getter
    @Setter
    @Nullable
    public DebugInfo debugInfo;

    @ToString
    @RequiredArgsConstructor
    @Getter
    static final class FieldValidationErrorDescription {
        private final String fieldName;
        private final String errorMessage;
    }
}
