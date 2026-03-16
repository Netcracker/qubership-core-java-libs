package com.netcracker.cloud.framework.quarkus.contexts.acceptlanuages;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;


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
