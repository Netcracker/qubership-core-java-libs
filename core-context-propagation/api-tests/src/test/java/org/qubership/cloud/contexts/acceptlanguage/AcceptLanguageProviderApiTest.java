package org.qubership.cloud.contexts.acceptlanguage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;


public class AcceptLanguageProviderApiTest {

    @BeforeEach
    public void setup(){
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    public void testDefaultConstructor() {
        final AcceptLanguageProvider languageProvider = new AcceptLanguageProvider();
        assertNotNull(languageProvider);
    }

    @Test
    public void testAcceptLanguageContextName() {
        assertEquals("Accept-Language", AcceptLanguageProvider.ACCEPT_LANGUAGE);
        assertEquals("Accept-Language", new AcceptLanguageProvider().contextName());
    }

    @Test
    public void testProvideDefaultAcceptLanguage() {
        AcceptLanguageProvider acceptLanguageProvider = new AcceptLanguageProvider();
        AcceptLanguageContextObject acceptLanguageContextObject = acceptLanguageProvider.provide(null);
        assertNull(acceptLanguageContextObject.getAcceptedLanguages());
    }

    @Test
    public void testProvideCustomAcceptLanguage() {
        AcceptLanguageProvider acceptLanguageProvider = new AcceptLanguageProvider();
        AcceptLanguageContextObject acceptLanguageContextObject = acceptLanguageProvider.provide(IncomingContextDataFactory.getAcceptLanguageIncomingContextData());
        assertEquals("ru; en", acceptLanguageContextObject.getAcceptedLanguages());
    }
}
