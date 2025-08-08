package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;
import java.util.regex.Matcher;

@Value
@With
@AllArgsConstructor
public class VersionedGroupId implements GroupId {
    String groupIdPrefix;
    Version version;
    State state;
    State siblingState;
    OffsetDateTime updated;

    @Override
    public String toString() {
        return String.format("%s-%s-%s_%s-%s", groupIdPrefix, version, state.getShortName(), siblingState.getShortName(), VersionedGroupId.fromOffsetDateTime(updated));
    }

    public static String fromOffsetDateTime(OffsetDateTime dateTime) {
        return String.format(datePartTemplate,
                dateTime.getYear(), dateTime.getMonth().getValue(), dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    public static OffsetDateTime toOffsetDateTime(String dateTime) {
        Matcher m = dateTimePattern.matcher(dateTime);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid dateTime format '%s'. Must match patter: '%s'", dateTime, dateTimePattern));
        }
        Function<String, Integer> toInt = Integer::parseInt;
        Integer year = toInt.apply(m.group("year"));
        Integer month = toInt.apply(m.group("month"));
        Integer day = toInt.apply(m.group("day"));
        Integer hour = toInt.apply(m.group("hour"));
        Integer minute = toInt.apply(m.group("minute"));
        Integer second = toInt.apply(m.group("second"));
        return OffsetDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC);
    }
}
