package com.netcracker.cloud.context.propagation.sample.threads;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider.ACCEPT_LANGUAGE;

class AcceptLanguageThreadsPropagationTest extends AbstractThreadTest {

    private static final String ACCEPT_LANG_VALUE = "en-US;";

    final Runnable runnableWithAcceptLang = () -> assertEquals(ACCEPT_LANG_VALUE, AcceptLanguageContext.get());
    final Runnable runnableWithoutAcceptLang = () -> assertNull(AcceptLanguageContext.get());


    @Test
    void testPropagationForAcceptLang() throws Exception {
        AcceptLanguageContext.set(ACCEPT_LANG_VALUE);
        simpleExecutor.submit(runnableWithAcceptLang).get();
    }

    @Test
    void testNoPropagationForAcceptLang() throws Exception {
        simpleExecutor.submit(runnableWithoutAcceptLang).get();
    }

    @Test
    void childThreadDoesntAffectParentOne() {
        ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject(ACCEPT_LANG_VALUE));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject("new-lang")));
        executor.shutdown();
        assertEquals(ACCEPT_LANG_VALUE, ((AcceptLanguageContextObject) ContextManager.get(ACCEPT_LANGUAGE)).getAcceptedLanguages());
    }

}
