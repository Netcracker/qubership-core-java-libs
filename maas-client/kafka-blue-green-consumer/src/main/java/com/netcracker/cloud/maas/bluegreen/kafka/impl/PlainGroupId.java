package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

@Value
@With
@AllArgsConstructor
public class PlainGroupId implements GroupId {

    String name;

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getGroupIdPrefix() {
        return this.name;
    }
}
