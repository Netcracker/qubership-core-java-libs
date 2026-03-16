package com.netcracker.cloud.bluegreen;

import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.net.InetAddress;

public class EnvUtilTest {

    @Test
    void testGetNamespaceErrorCase() {
        Assertions.assertThrows(IllegalStateException.class, EnvUtil::getNamespace,
                "Missing required prop: 'cloud.microservice.namespace' or env(s): 'NAMESPACE, CLOUD_NAMESPACE'");
    }

    @Test
    @SetSystemProperty(key = "cloud.microservice.namespace", value = "namespace-from-property")
    void testGetNamespaceFromProp() {
        String namespace = EnvUtil.getNamespace();
        Assertions.assertEquals("namespace-from-property", namespace);
    }

    @Test
    void testGetMicroserviceNameErrorCase() {
        Assertions.assertThrows(IllegalStateException.class, EnvUtil::getMicroserviceName,
                "Missing required prop: 'cloud.microservice.name' or env(s): 'SERVICE_NAME'");
    }

    @Test
    @SetSystemProperty(key = "cloud.microservice.name", value = "name-from-property")
    void testGetNameFromProp() {
        String name = EnvUtil.getMicroserviceName();
        Assertions.assertEquals("name-from-property", name);
    }

    @Test
    void testGetConsulUrlErrorCase() {
        Assertions.assertThrows(IllegalStateException.class, EnvUtil::getConsulUrl,
                "Missing required prop: 'consul.url' or env(s): 'CONSUL_URL'");
    }

    @Test
    @SetSystemProperty(key = "consul.url", value = "url-from-property")
    void testGetConsulUrlFromProp() {
        String url = EnvUtil.getConsulUrl();
        Assertions.assertEquals("url-from-property", url);
    }

    @Test
    void testGetPodName() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName();
        String podName = EnvUtil.getPodName();
        Assertions.assertEquals(hostName, podName);
    }

    @Test
    @SetSystemProperty(key = "pod.name", value = "pod-from-property")
    void testGetPodNameFromProp() {
        String podName = EnvUtil.getPodName();
        Assertions.assertEquals("pod-from-property", podName);
    }
}
