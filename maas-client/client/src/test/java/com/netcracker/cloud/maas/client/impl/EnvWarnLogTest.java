package com.netcracker.cloud.maas.client.impl;

import org.qubership.cloud.maas.client.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.qubership.cloud.maas.client.Utils.withProp;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

class EnvWarnLogTest {

    private ByteArrayOutputStream outContent;
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut((new PrintStream(new Utils.DoubleOutputStream(outContent, originalOut))));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testNamespaceOldPropLog() {
        withProp(Env.PROP_NAMESPACE, "testNamespaceOldPropLog", () -> {
            Env.namespace();
            String actualLog = outContent.toString();
            Assertions.assertTrue(actualLog.contains(getWarnLogString(Env.PROP_NAMESPACE, Env.PROP_CLOUD_NAMESPACE)));
        });
    }

    @Test
    void testNamespaceNewAndOldProp() {
        withProp(Env.PROP_CLOUD_NAMESPACE, "testNamespaceNewAndOldProp-new", () -> {
            withProp(Env.PROP_NAMESPACE, "testNamespaceNewAndOldProp-old", () -> {
                Env.namespace();
                String actualLog = outContent.toString();
                Assertions.assertFalse(actualLog.contains(getWarnLogString(Env.PROP_NAMESPACE, Env.PROP_CLOUD_NAMESPACE)));
            });
        });
    }

    @Test
    void testNamespaceNewAndOldPropNewEnv() throws Exception {
        withProp(Env.PROP_NAMESPACE, null, () -> {
            withEnvironmentVariable(Env.ENV_CLOUD_NAMESPACE, "testNamespaceNewAndOldPropNewEnv").execute(() -> Env.namespace());
            String actualLog = outContent.toString();
            Assertions.assertFalse(actualLog.contains(getWarnLogString(Env.PROP_NAMESPACE, Env.PROP_CLOUD_NAMESPACE)));
        });
    }

    private String getWarnLogString(String deprecated, String alternative) {
        return String.format("Using '%s' is deprecated. Migrate to '%s'", deprecated, alternative);
    }
}
