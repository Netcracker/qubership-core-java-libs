package org.qubership.cloud.quarkus.routesregistration.deployment.resource.hierarchy;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.Path;

@Route
public class Case1 extends Case3 implements ICase1, ICase2 {
    @Override
    @Route
    @Path("/case1/method1")
    public void method1() {

    }

    @Route
    @Override
    public void method2() {

    }

    @Route
    @Override
    public void method3() {

    }

    @Override
    public void method5() {

    }

    @Route
    @Override
    public void method4() {

    }

    @Route
    @Override
    public void method4(String s) {

    }

    @Route
    @Override
    @Path("/case1/method4Integer")
    public void method4(Integer i) {

    }

    @Route
    public void case2Method1() {

    }

    @Route
    @Override
    @Path("/case1/case2Method2")
    public void case2Method2() {

    }
}
