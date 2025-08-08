package org.qubership.cloud.framework.quarkus.contexts.businessprocess;

import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.supports.strategies.RestEasyDefaultStrategy;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessContextObject;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessProvider;

@RegisterProvider
public class QuarkusBusinessProcessProvider extends BusinessProcessProvider {

    RestEasyDefaultStrategy<BusinessProcessContextObject> businessProcessStrategy = new RestEasyDefaultStrategy<>(BusinessProcessContextObject.class, defaultContextObject());

    @Override
    public Strategy<BusinessProcessContextObject> strategy() {
        return businessProcessStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
