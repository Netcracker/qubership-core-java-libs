package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import jakarta.ws.rs.core.HttpHeaders;


public class AcceptLanguageContext {

    public static String get() {
        AcceptLanguageContextObject acceptLanguageContextObject = ContextManager.get(HttpHeaders.ACCEPT_LANGUAGE);
        return acceptLanguageContextObject.getAcceptedLanguages();
    }

    public static void set(String newAcceptLanguage) {
        AcceptLanguageContextObject acceptLanguageContextObject = new AcceptLanguageContextObject(newAcceptLanguage);
        ContextManager.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguageContextObject);
    }

    public static void clear() {
        ContextManager.clear(HttpHeaders.ACCEPT_LANGUAGE);
    }
}
