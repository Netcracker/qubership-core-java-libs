package org.qubership.cloud.framework.contexts.businessprocess;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

@RegisterProvider
public class BusinessProcessProvider extends AbstractContextProviderOnInheritableThreadLocal<BusinessProcessContextObject> {

    public static final String CONTEXT_NAME = "Business-Process-Id";

    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    @Override
    public BusinessProcessContextObject provide(@Nullable IncomingContextData contextData) {
        return new BusinessProcessContextObject(contextData);
    }

}
