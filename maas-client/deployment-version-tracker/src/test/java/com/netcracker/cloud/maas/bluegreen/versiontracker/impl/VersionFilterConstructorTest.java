package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.qubership.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor.constructVersionFilter;
import static org.junit.jupiter.api.Assertions.*;

class VersionFilterConstructorTest {

    @Test
    public void testStateNoSibling() {
        Arrays.stream(State.values()).forEach(blueGreenStatus -> {
            BlueGreenState state = new BlueGreenState(new NamespaceVersion("namespace-1", blueGreenStatus, new Version(1)), OffsetDateTime.now());

            Predicate<String> p = constructVersionFilter(state);

            assertEquals("true", p.toString());
            assertTrue(p.test("any-version"));
        });
    }

    @Test
    public void testStateActiveIdle() {
        BlueGreenState state = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(1)),
                new NamespaceVersion("namespace-2", State.IDLE, null),
                OffsetDateTime.now());

        Predicate<String> p = constructVersionFilter(state);

        assertEquals("true", p.toString());
        assertTrue(p.test("any-version"));
    }

    @Test
    public void testStateActiveCandidate() {
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
    public void testStateActiveLegacy() {
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
    public void testStateCandidateActive() {
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
    public void testStateLegacyActive() {
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
}
