package org.qubership.cloud.restlegacy.restclient.error.v2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
class CustomExceptionResponse implements HasDebugInfo<DebugInfo> {
    @Getter
    private final String message;

    @Getter
    private final String customData;

    @Setter
    @Getter
    public DebugInfo debugInfo;
}
