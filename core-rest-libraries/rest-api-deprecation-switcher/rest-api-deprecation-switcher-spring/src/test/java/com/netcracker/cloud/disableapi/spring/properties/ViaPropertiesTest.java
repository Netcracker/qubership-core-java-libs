package com.netcracker.cloud.disableapi.spring.properties;

import com.netcracker.cloud.disableapi.spring.AbstractTestParent;
import com.netcracker.cloud.disableapi.spring.DisableDeprecatedApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ViaPropertiesTest.Application.class},
        properties = {"deprecated.api.disabled=true",
                "deprecated.api.patterns[0]=/api/v1/** [GET PUT DELETE]",
                "deprecated.api.patterns[1]=/api/v2/**"})
class ViaPropertiesTest extends AbstractTestParent {

    @SpringBootApplication
    @DisableDeprecatedApi
    @ComponentScan(basePackageClasses = {
            ControllerV1.class,
            ControllerV2.class,
            ControllerV3.class,
    })
    public static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

    @Test
    void v1Test() {
        test("/api/v1/test", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v1/test", HttpMethod.POST, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v1/test/inner", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v1/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v1/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
    }

    @Test
    void v2Test() {
        test("/api/v2/test", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v2/test", HttpMethod.POST, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v2/test/inner", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v2/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
        test("/api/v2/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.NOT_FOUND, ERROR_RESPONSE_STRING);
    }

    @Test
    void v3Test() {
        test("/api/v3/test", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test", HttpMethod.POST, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner/wildcard/param1", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
        test("/api/v3/test/inner/extension/example.html", HttpMethod.GET, HttpStatus.OK, SUCCESS_RESPONSE_STRING);
    }

    @RestController
    @RequestMapping("/api/v1/test")
    public static class ControllerV1 {
        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiPost() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/wildcard/{param}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithParamGet(@PathVariable String param) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/extension/{name}.html", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithExtensionGet(@PathVariable String name) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

    }

    @RestController
    @RequestMapping("/api/v2/test")
    public static class ControllerV2 {
        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiPost() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/wildcard/{param}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithParamGet(@PathVariable String param) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/extension/{name}.html", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithExtensionGet(@PathVariable String name) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }
    }

    @RestController
    @RequestMapping("/api/v3/test")
    public static class ControllerV3 {
        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiPost() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/wildcard/{param}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithParamGet(@PathVariable String param) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.GET, path = "/inner/extension/{name}.html", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> apiInnerWithExtensionGet(@PathVariable String name) {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }
    }
}
