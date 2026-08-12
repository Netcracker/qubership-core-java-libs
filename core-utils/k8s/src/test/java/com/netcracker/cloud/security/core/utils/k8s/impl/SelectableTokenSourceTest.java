package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelectableTokenSourceTest {

    @AfterEach
    void reset() {
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
    }

    @Test
    void delegatesToInjectedTokenSource() throws Exception {
        TokenSource delegate = mock(TokenSource.class);
        when(delegate.getToken("netcracker")).thenReturn("minted");

        try (SelectableTokenSource source = new SelectableTokenSource(delegate)) {
            assertEquals("minted", source.getToken("netcracker"));
        }
        verify(delegate).getToken("netcracker");
        verify(delegate).close();
    }

    @Test
    void constructsWhenLocalDevEnabled() throws Exception {
        System.setProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY, LocalDevMode.DEV_PROFILE);
        try (SelectableTokenSource ignored = new SelectableTokenSource()) {
            // construction selects local-dev delegate
        }
    }
}
