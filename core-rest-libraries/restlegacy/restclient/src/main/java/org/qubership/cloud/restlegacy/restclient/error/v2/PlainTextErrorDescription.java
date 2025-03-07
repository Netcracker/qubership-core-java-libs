package org.qubership.cloud.restlegacy.restclient.error.v2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.annotation.Nullable;


@ToString
@RequiredArgsConstructor
public class PlainTextErrorDescription implements HasDebugInfo<DebugInfo> {
    @Getter
    private final String errorMessage;

    @Getter
    @Setter
    @Nullable
    public DebugInfo debugInfo;
}
