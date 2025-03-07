package org.qubership.cloud.routesregistration.common.spring.gateway.route;


import org.springframework.beans.factory.FactoryBean;

public class TestController7 extends TestControllerBaseFor6 implements FactoryBean<Void> {

    @Override
    public void method1() {
    }

    public Void getObject() throws Exception {
        return null;
    }

    public Class<? extends Void> getObjectType() {
        return null;
    }

    public boolean isSingleton() {
        return true;
    }

}
