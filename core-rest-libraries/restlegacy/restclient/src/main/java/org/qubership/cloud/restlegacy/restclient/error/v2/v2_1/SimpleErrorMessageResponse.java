package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import org.qubership.cloud.restlegacy.restclient.error.v2.DebugInfo;
import org.qubership.cloud.restlegacy.restclient.error.v2.HasDebugInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.annotation.Nullable;

@ToString
@RequiredArgsConstructor
public class SimpleErrorMessageResponse implements HasDebugInfo<DebugInfo> {
    @Getter
    private final ErrorMessage errorMessage;

    @Getter
    @Setter
    @Nullable
    DebugInfo debugInfo;
}
