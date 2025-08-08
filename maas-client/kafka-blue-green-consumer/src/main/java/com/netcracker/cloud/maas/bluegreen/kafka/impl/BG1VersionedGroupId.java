package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.bluegreen.api.model.Version;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.time.OffsetDateTime;

@Value
@With
@AllArgsConstructor
public class BG1VersionedGroupId implements GroupId {

    public static final String ACTIVE_STAGE = "a";
    public static final String MIGRATED_STAGE = "M";

    String groupIdPrefix;
    Version version;
    Version blueGreenVersion;
    String stage;
    OffsetDateTime time;

    @Override
    public String toString() {
        return String.format("%s-%s%s%s%d", groupIdPrefix, version, blueGreenVersion, stage, time.toEpochSecond());
    }
}
