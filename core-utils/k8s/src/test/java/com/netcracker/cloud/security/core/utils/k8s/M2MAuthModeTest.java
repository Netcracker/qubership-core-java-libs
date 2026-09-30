package com.netcracker.cloud.security.core.utils.k8s;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class M2MAuthModeTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource(delimiter = '|', value = {
            "legacy|LEGACY",
            "hybrid|HYBRID",
            "k8s|K8S",
            "Hybrid|HYBRID",
            "' K8S\t'|K8S",
    })
    void parse(String value, M2MAuthMode expected) {
        assertEquals(expected, M2MAuthMode.parse(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void parse_MissingValueIsLegacy(String value) {
        assertEquals(M2MAuthMode.LEGACY, M2MAuthMode.parse(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "kubernetes"})
    void parse_UnsupportedValueIsRejected(String value) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> M2MAuthMode.parse(value));

        assertTrue(e.getMessage().startsWith("M2M_AUTH_MODE has unsupported value \"" + value + "\""), e.getMessage());
    }

    @Test
    void read_UsesTheEnvironmentVariable() {
        environmentVariables.set(M2MAuthMode.ENV, "k8s");

        assertEquals(M2MAuthMode.K8S, M2MAuthMode.read());
    }

    @Test
    void read_UnsetVariableIsLegacy() {
        environmentVariables.remove(M2MAuthMode.ENV);

        assertEquals(M2MAuthMode.LEGACY, M2MAuthMode.read());
    }
}
