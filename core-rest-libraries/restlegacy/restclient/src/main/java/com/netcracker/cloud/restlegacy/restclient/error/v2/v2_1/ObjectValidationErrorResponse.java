package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import com.netcracker.cloud.restlegacy.restclient.error.v2.DebugInfo;
import com.netcracker.cloud.restlegacy.restclient.error.v2.HasDebugInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.annotation.Nullable;

@ToString
@RequiredArgsConstructor
public class ObjectValidationErrorResponse implements HasDebugInfo<DebugInfo> {

    @Getter
    private final ErrorMessage errorMessage;

    @Getter
    @Nullable
    private final FieldValidationMessage[] fieldValidationMessages;

    @Getter
    @Setter
    @Nullable
    DebugInfo debugInfo;
}
