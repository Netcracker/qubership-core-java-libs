package com.netcracker.cloud.contexts.acceptlanguage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcceptLanguageContextObjectApiTest {

    @BeforeEach
    void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    void testGetAcceptedApi() {
        AcceptLanguageContextObject acceptLanguageContextObject = new AcceptLanguageContextObject("ru");
        String acceptLanguage = acceptLanguageContextObject.getAcceptedLanguages(); // API
        assertEquals("ru", acceptLanguage);
    }

    @Test
    void testGetAcceptLanguageFromContextManager() {
        ContextManager.register(Collections.singletonList(new AcceptLanguageProvider()));
        RequestContextPropagation.initRequestContext(IncomingContextDataFactory.getAcceptLanguageIncomingContextData());
        AcceptLanguageContextObject acceptLanguageContextObject = ContextManager.get(AcceptLanguageProvider.ACCEPT_LANGUAGE); // API
        assertEquals("ru; en", acceptLanguageContextObject.getAcceptedLanguages());
        ContextManager.clearAll();
    }
}
