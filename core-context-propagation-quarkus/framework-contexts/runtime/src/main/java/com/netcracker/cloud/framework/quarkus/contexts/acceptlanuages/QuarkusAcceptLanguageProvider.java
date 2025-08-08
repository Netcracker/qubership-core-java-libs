package com.netcracker.cloud.framework.quarkus.contexts.acceptlanuages;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;


@RegisterProvider
public class QuarkusAcceptLanguageProvider extends AcceptLanguageProvider {
    RestEasyDefaultStrategy<AcceptLanguageContextObject> acceptLanguageStrategy = new RestEasyDefaultStrategy<>(AcceptLanguageContextObject.class, defaultContextObject());

    @Override
    public Strategy<AcceptLanguageContextObject> strategy() {
        return acceptLanguageStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
