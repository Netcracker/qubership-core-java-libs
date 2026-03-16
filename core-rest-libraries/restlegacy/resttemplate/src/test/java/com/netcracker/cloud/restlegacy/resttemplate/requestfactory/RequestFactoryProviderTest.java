package com.netcracker.cloud.restlegacy.resttemplate.requestfactory;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.ClientHttpRequestFactoryConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(classes = {ClientHttpRequestFactoryConfiguration.class, TestRequestFactoryProviderImpl.class})
class RequestFactoryProviderTest {

    @Autowired
    @Qualifier("clientHttpRequestFactory")
    ClientHttpRequestFactory clientHttpRequestFactory;

    @Test
    void maxTotalTest() {
        assertInstanceOf(BufferingClientHttpRequestFactory.class, clientHttpRequestFactory);
    }
}
