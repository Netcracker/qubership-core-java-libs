package org.qubership.cloud.context.propagation.core.contextdata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DeserializedIncomingContextDataTest {

    @Test
    void getIncomingData() {
        DeserializedIncomingContextData incomingContextData =
                new DeserializedIncomingContextData(Map.of(
                        "serField", "serData",
                        "X-Test-Case-Header","case-insensitive-header-value"));

        Assertions.assertEquals("serData", incomingContextData.get("serField"));
        Assertions.assertEquals("case-insensitive-header-value", incomingContextData.get("x-test-case-header"));
    }

    @Test
    void getAllNotSupported() {
        DeserializedIncomingContextData incomingContextData =
                new DeserializedIncomingContextData(Map.of());

        Assertions.assertThrows(UnsupportedOperationException.class, incomingContextData::getAll);
    }
}