package org.qubership.cloud.restlegacy.resttemplate.requestfactory;

import org.qubership.cloud.restlegacy.resttemplate.configuration.ClientHttpRequestFactoryConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ClientHttpRequestFactoryConfiguration.class, TestRequestFactoryProviderImpl.class})
public class RequestFactoryProviderTest {

    @Autowired
    @Qualifier("clientHttpRequestFactory")
    ClientHttpRequestFactory clientHttpRequestFactory;

    @Test
    public void maxTotalTest(){
        Assertions.assertTrue(clientHttpRequestFactory instanceof BufferingClientHttpRequestFactory);
    }
}
