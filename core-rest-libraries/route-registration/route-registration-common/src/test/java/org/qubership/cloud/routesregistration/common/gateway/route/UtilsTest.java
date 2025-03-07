package org.qubership.cloud.routesregistration.common.gateway.route;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void formatMicroserviceInternalURLTest(){
        String result = Utils.formatMicroserviceInternalURL("cloudTest", "nameTest", "portTest", "/contextTest", false);
        Assert.assertEquals("http://cloudTest:portTest/contextTest", result);
    }
}
