package com.netcracker.cloud.bluegreen.impl.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeDeserializer;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BGState {
    NSVersion originNamespace;
    NSVersion peerNamespace;

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    OffsetDateTime updateTime;
}

