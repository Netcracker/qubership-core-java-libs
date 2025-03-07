package org.qubership.cloud.disableapi.spring.annotations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AbstractAnnotationTestParent.Application.class},
        properties = {"deprecated.api.disabled=false"})
class DisabledTest extends AbstractAnnotationTestParent {

    @Test
    void v1Test() {
        test("/api/v1/test", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v1/test", HttpMethod.POST, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v1/test/inner", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v1/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v1/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
    }

    @Test
    void v2Test() {
        test("/api/v2/test", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v2/test", HttpMethod.POST, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v2/test/inner", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v2/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v2/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
    }

    @Test
    void v3Test() {
        test("/api/v3/test", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test", HttpMethod.POST, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
    }
}
