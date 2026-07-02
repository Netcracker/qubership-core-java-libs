package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.function.Predicate;

import static com.netcracker.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor.constructVersionFilter;
import static com.netcracker.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor.constructVersionNameFilter;
import static org.junit.jupiter.api.Assertions.*;

class VersionFilterConstructorTest {

    @Test
    void testStateNoSibling() {
        Arrays.stream(State.values()).forEach(blueGreenStatus -> {
            BlueGreenState state = new BlueGreenState(new NamespaceVersion("namespace-1", blueGreenStatus, new Version(1)), OffsetDateTime.now());

            Predicate<String> p = constructVersionFilter(state);

            assertEquals("true", p.toString());
            assertTrue(p.test("any-version"));
        });
    }

    @Test
    void testStateActiveIdle() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(1)),
                new NamespaceVersion("namespace-2", State.IDLE, null),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("true", p.toString());
        assertTrue(p.test("any-version"));
    }

    @Test
    void testStateActiveCandidate() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(3)),
                new NamespaceVersion("namespace-2", State.CANDIDATE, new Version(4)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("!v4", p.toString());
        assertFalse(p.test("v4"));
        assertTrue(p.test("v3"));
        assertTrue(p.test("v2"));
        assertTrue(p.test("v1"));
        assertTrue(p.test(""));
    }

    @Test
    void testStateActiveLegacy() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(4)),
                new NamespaceVersion("namespace-2", State.LEGACY, new Version(3)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("!v3", p.toString());
        assertFalse(p.test("v3"));
        assertTrue(p.test("v4"));
        assertTrue(p.test("v2"));
        assertTrue(p.test("v1"));
        assertTrue(p.test(""));
    }

    @Test
    void testStateCandidateActive() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.CANDIDATE, new Version(4)),
                new NamespaceVersion("namespace-2", State.ACTIVE, new Version(3)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("v4", p.toString());
        assertTrue(p.test("v4"));
        assertFalse(p.test("v3"));
        assertFalse(p.test("v2"));
        assertFalse(p.test("v1"));
        assertFalse(p.test(""));
    }

    @Test
    void testStateLegacyActive() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.LEGACY, new Version(3)),
                new NamespaceVersion("namespace-2", State.ACTIVE, new Version(4)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("v3", p.toString());
        assertTrue(p.test("v3"));
        assertFalse(p.test("v4"));
        assertFalse(p.test("v2"));
        assertFalse(p.test("v1"));
        assertFalse(p.test(""));
    }

    @Test
    void testVersionNameNoSibling() {
        Arrays.stream(State.values()).forEach(blueGreenStatus -> {
            BlueGreenState state = new BlueGreenState(new NamespaceVersion("namespace-1", blueGreenStatus, new Version(1)), OffsetDateTime.now());

            Predicate<String> p = constructVersionNameFilter(state);

            assertEquals("true", p.toString());
            assertTrue(p.test("any-name"));
        });
    }

    @Test
    void testVersionNameActiveIdle() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(1)),
                new NamespaceVersion("namespace-2", State.IDLE, null),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionNameFilter(state);

        assertEquals("true", p.toString());
        assertTrue(p.test("candidate"));
    }

    @Test
    void testVersionNameActiveCandidate() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(3)),
                new NamespaceVersion("namespace-2", State.CANDIDATE, new Version(4)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionNameFilter(state);

        assertEquals("!candidate", p.toString());
        assertFalse(p.test("candidate"));
        assertFalse(p.test("CANDIDATE"));
        assertTrue(p.test("active"));
        assertTrue(p.test("legacy"));
    }

    @Test
    void testVersionNameActiveLegacy() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(4)),
                new NamespaceVersion("namespace-2", State.LEGACY, new Version(3)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionNameFilter(state);

        assertEquals("!legacy", p.toString());
        assertFalse(p.test("legacy"));
        assertTrue(p.test("active"));
        assertTrue(p.test("candidate"));
    }

    @Test
    void testVersionNameCandidateActive() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.CANDIDATE, new Version(4)),
                new NamespaceVersion("namespace-2", State.ACTIVE, new Version(3)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionNameFilter(state);

        assertEquals("candidate", p.toString());
        assertTrue(p.test("candidate"));
        assertTrue(p.test("CANDIDATE"));
        assertFalse(p.test("active"));
        assertFalse(p.test("legacy"));
        assertFalse(p.test(""));
    }

    @Test
    void testVersionNameLegacyActive() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.LEGACY, new Version(3)),
                new NamespaceVersion("namespace-2", State.ACTIVE, new Version(4)),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionNameFilter(state);

        assertEquals("legacy", p.toString());
        assertTrue(p.test("legacy"));
        assertFalse(p.test("active"));
        assertFalse(p.test("candidate"));
        assertFalse(p.test(""));
    }
}
