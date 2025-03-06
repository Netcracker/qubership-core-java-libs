package org.qubership.cloud.framework.contexts.acceptlanguage;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.DefaultValueAwareContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;

/**
 * Associates a given accept language with the current execution thread. The purpose of the class is
 * to provide a convenient way to manage ACCEPT_LANGUAGE header.
 */
public class AcceptLanguageContextObject implements SerializableContext,
        DefaultValueAwareContext<String>, SerializableDataContext {

    private String acceptedLanguages;

    public AcceptLanguageContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(ACCEPT_LANGUAGE) != null) {
            this.acceptedLanguages = (String) contextData.get(ACCEPT_LANGUAGE);
        } else {
            this.acceptedLanguages = getDefault();
        }
    }

    public AcceptLanguageContextObject(String acceptedLanguages) {
        this.acceptedLanguages = acceptedLanguages;
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        if (acceptedLanguages != null)
            outgoingContextData.set(ACCEPT_LANGUAGE, acceptedLanguages);
    }

    public String getAcceptedLanguages() {
        return acceptedLanguages;
    }

    @Override
    public String getDefault() {
        return  ((RequestContextObject) ContextManager.get("request")).getFirst(ACCEPT_LANGUAGE);
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (acceptedLanguages != null) {
            return Map.of(ACCEPT_LANGUAGE, acceptedLanguages);
        }
        return Collections.emptyMap();
    }
}
