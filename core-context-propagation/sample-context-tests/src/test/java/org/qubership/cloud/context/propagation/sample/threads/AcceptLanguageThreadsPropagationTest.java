package org.qubership.cloud.context.propagation.sample.threads;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider.ACCEPT_LANGUAGE;
import static org.junit.Assert.*;

public class AcceptLanguageThreadsPropagationTest extends AbstractThreadTest {

    private static final String ACCEPT_LANG_VALUE = "en-US;";
    private static final Map<String, String> ALLOWED_HEADERS = Collections.singletonMap("custom_header", "val");

    final Runnable runnableWithAcceptLang = () -> assertEquals(ACCEPT_LANG_VALUE, AcceptLanguageContext.get());
    final Runnable runnableWithoutAcceptLang = () -> assertNull(AcceptLanguageContext.get());


    @Test
    public void testPropagationForAcceptLang() throws Exception {
        AcceptLanguageContext.set(ACCEPT_LANG_VALUE);
        simpleExecutor.submit(runnableWithAcceptLang).get();
    }

    @Test
    public void testNoPropagationForAcceptLang() throws Exception {
        simpleExecutor.submit(runnableWithoutAcceptLang).get();
    }

    @Test
    public void childThreadDoesntAffectParentOne() {
        ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject(ACCEPT_LANG_VALUE));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject("new-lang"));
        });
        executor.shutdown();
        assertEquals(ACCEPT_LANG_VALUE, ((AcceptLanguageContextObject) ContextManager.get(ACCEPT_LANGUAGE)).getAcceptedLanguages());
    }

}
