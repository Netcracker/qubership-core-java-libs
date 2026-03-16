package com.netcracker.cloud.disableapi.spring.annotations;

import com.netcracker.cloud.disableapi.spring.AbstractTestParent;
import com.netcracker.cloud.disableapi.spring.DisableDeprecatedApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

public abstract class AbstractAnnotationTestParent extends AbstractTestParent {
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
    @RestController
    @RequestMapping("/api/v1/test")
    @Deprecated
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
        @Deprecated
        public ResponseEntity<String> apiGet() {
            return new ResponseEntity<>("ok", HttpStatus.OK);
        }

        @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
        @Deprecated
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
