package com.netcracker.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netcracker.cloud.bluegreen.api.model.LockAction;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeDeserializer;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Data
@NoArgsConstructor
public class ModifyLockData {
    private LockAction action;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime timestamp;
    private String modifier;

    public ModifyLockData(LockAction action, String namespace, String name) {
        this.action = action;
        this.timestamp = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC);
        this.modifier = String.format("%s mutex in '%s/%s'", action.name().toLowerCase(), namespace, name);
    }
}
