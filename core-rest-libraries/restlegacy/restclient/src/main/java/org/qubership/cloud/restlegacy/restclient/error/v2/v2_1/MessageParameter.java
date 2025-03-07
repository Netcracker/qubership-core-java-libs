package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MessageParameter {
    private final String type;

    @JsonSerialize(using = MessageParameterValueSerializer.class)
    private final Object value;
}
