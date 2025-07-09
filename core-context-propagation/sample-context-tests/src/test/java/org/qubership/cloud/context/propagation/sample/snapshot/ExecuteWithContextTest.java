package org.qubership.cloud.context.propagation.sample.snapshot;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecuteWithContextTest {

    @Test
    void executeWithContextTest() {
        String initialContextValue = "first";
        String newContextValue = "second";
        AcceptLanguageContext.set(initialContextValue);

        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        AcceptLanguageContext.set(newContextValue);
        assertEquals(newContextValue, ((AcceptLanguageContextObject) ContextManager.get(HttpHeaders.ACCEPT_LANGUAGE)).getAcceptedLanguages());

        ContextManager.executeWithContext(contextSnapshot, () -> {
            assertEquals(initialContextValue, ((AcceptLanguageContextObject) ContextManager.get(HttpHeaders.ACCEPT_LANGUAGE)).getAcceptedLanguages());
            return null;
        });

        assertEquals(newContextValue, ((AcceptLanguageContextObject) ContextManager.get(HttpHeaders.ACCEPT_LANGUAGE)).getAcceptedLanguages());
    }
}
