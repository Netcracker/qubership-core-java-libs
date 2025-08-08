package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface GroupId {

    String stateCharPattern = Arrays.stream(State.values())
            .map(State::getShortName)
            .collect(Collectors.joining("", "", ""));
    String dateTimePatternString = "(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})_(?<hour>\\d{2})-(?<minute>\\d{2})-(?<second>\\d{2})";
    Pattern dateTimePattern = Pattern.compile(dateTimePatternString);
    Pattern versionedPattern = Pattern.compile("(?<groupPrefix>.+)-(?<version>v\\d+)-(?<state>[" + stateCharPattern + "])_(?<siblingState>[" + stateCharPattern + "])-(?<datetime>" + dateTimePattern + ")");
    Pattern bg1VersionedPattern = Pattern.compile("(?<groupPrefix>.+)-(?<version>v\\d+)(?<blueGreenVersion>v\\d+)(?<stage>[lacrM])(?<time>\\d+)");
    String datePartTemplate = "%02d-%02d-%02d_%02d-%02d-%02d";

    String getGroupIdPrefix();

    static GroupId parse(String name) {
        Objects.requireNonNull(name);
        Matcher m = versionedPattern.matcher(name);
        if (m.matches()) {
            String groupPrefix = m.group("groupPrefix");
            Version version = new Version(m.group("version"));
            State state = State.fromShort(m.group("state"));
            State siblingState = State.fromShort(m.group("siblingState"));
            OffsetDateTime datetime = VersionedGroupId.toOffsetDateTime(m.group("datetime"));
            return new VersionedGroupId(groupPrefix, version, state, siblingState, datetime);
        } else {
            m = bg1VersionedPattern.matcher(name);
            if (m.matches()) {
                String groupPrefix = m.group("groupPrefix");
                Version version = new Version(m.group("version"));
                Version blueGreenVersion = new Version(m.group("blueGreenVersion"));
                String stage = m.group("stage");
                long t = Long.parseLong(m.group("time"));
                OffsetDateTime time = OffsetDateTime.ofInstant(Instant.ofEpochSecond(t), ZoneOffset.UTC);
                return new BG1VersionedGroupId(groupPrefix, version, blueGreenVersion, stage, time);
            } else {
                return new PlainGroupId(name);
            }
        }
    }
}
