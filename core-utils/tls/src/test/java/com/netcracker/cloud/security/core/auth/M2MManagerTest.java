package com.netcracker.cloud.security.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class M2MManagerTest {

    @Test
    void priority() {
        M2MManager m2MManager = () -> Token.DUMMY_TOKEN;
        assertEquals(0, m2MManager.priority());
    }

    @Test
    void loadPreferredTakesTheManagerWithTheHighestPriority() {
        assertInstanceOf(TestM2MManager.class, M2MManager.loadPreferred());
    }
}
