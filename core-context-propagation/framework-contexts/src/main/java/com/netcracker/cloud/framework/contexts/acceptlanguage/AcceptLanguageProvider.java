package com.netcracker.cloud.framework.contexts.acceptlanguage;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class AcceptLanguageProvider extends AbstractContextProviderOnInheritableThreadLocal<AcceptLanguageContextObject> {
    public final static String ACCEPT_LANGUAGE = "Accept-Language";

    @Override
    public final String contextName() {
        return ACCEPT_LANGUAGE;
    }

    @Override
    public AcceptLanguageContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new AcceptLanguageContextObject(incomingContextData);
    }
}
