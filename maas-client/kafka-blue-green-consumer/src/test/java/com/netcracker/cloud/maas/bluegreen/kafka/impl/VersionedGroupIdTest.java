package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class VersionedGroupIdTest {
    @Test
    public void testSerialization() {
        String id = new VersionedGroupId("orders", new Version("v2"), State.LEGACY, State.ACTIVE,
                OffsetDateTime.of(LocalDate.of(2023, 8, 25), LocalTime.of(13, 30), ZoneOffset.UTC)).toString();
        assertEquals("orders-v2-l_a-2023-08-25_13-30-00", id);
    }

    @Test
    public void testStringifyViceVersa() {
        var id = new VersionedGroupId("orders", new Version("v5"), State.LEGACY, State.ACTIVE,
                OffsetDateTime.of(LocalDate.of(2023, 8, 25), LocalTime.of(13, 30, 15), ZoneOffset.UTC));
        assertEquals(id, GroupId.parse(id.toString()));
    }

    @Test
    public void testToString() {
        assertEquals("orders-v1-a_c-2023-08-25_13-30-00", GroupId.parse("orders-v1-a_c-2023-08-25_13-30-00").toString());
    }

    @Test
    public void testParse() {
        GroupId g = GroupId.parse("orders-v5-a_c-2023-08-25_13-30-15");
        assertTrue(g instanceof VersionedGroupId);
        VersionedGroupId v = (VersionedGroupId) g;
        assertEquals("orders", v.getGroupIdPrefix());
        assertEquals(new Version("v5"), v.getVersion());
        assertEquals(State.ACTIVE, v.getState());
        assertEquals(State.CANDIDATE, v.getSiblingState());
        assertEquals(OffsetDateTime.of(LocalDate.of(2023, 8, 25),
                        LocalTime.of(13, 30, 15), ZoneOffset.UTC),
                v.getUpdated());
    }

    @Test
    public void testTypes() {
        assertTrue(GroupId.parse("orders-v1-a_c-2023-08-25_13-30-00") instanceof VersionedGroupId);
        assertTrue(GroupId.parse("orders") instanceof PlainGroupId);
        assertTrue(GroupId.parse("orders-v1") instanceof PlainGroupId);
        assertTrue(GroupId.parse("orders-v1-a") instanceof PlainGroupId);
        assertTrue(GroupId.parse("orders-v1-a-2023-08-25_13-30-00") instanceof PlainGroupId);
    }

    @Test
    public void oldVersionedGroup() {
        assertTrue(GroupId.parse("orders-v1v1a1701191991") instanceof BG1VersionedGroupId);
        assertTrue(GroupId.parse("orders-v1v1c1701191991") instanceof BG1VersionedGroupId);
        assertTrue(GroupId.parse("orders-v1v1l1701191991") instanceof BG1VersionedGroupId);
        assertTrue(GroupId.parse("orders-v1v1r1701191991") instanceof BG1VersionedGroupId);
        assertFalse(GroupId.parse("orders-v1v1a1701191991") instanceof PlainGroupId);
        assertFalse(GroupId.parse("orders-v1v1a1701191991") instanceof VersionedGroupId);
    }
}
