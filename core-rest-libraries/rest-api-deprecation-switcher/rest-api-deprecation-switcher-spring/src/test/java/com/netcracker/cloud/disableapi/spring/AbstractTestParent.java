package com.netcracker.cloud.disableapi.spring;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class AbstractTestParent {
    public static String ERROR_RESPONSE_STRING = "is declined with 404 Not Found, because the following deprecated";
    public static String SUCCESS_RESPONSE_STRING = "ok";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    private String getUrl(String uri) {
        return String.format("http://localhost:%d%s", port, uri);
    }

    protected void test(String uri, HttpMethod method, HttpStatus expectedStatus, String text) {
        ResponseEntity<String> response = this.restTemplate.exchange(getUrl(uri), method, HttpEntity.EMPTY, String.class);
        Assertions.assertEquals(expectedStatus, response.getStatusCode());
        String body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains(text));
    }
}
