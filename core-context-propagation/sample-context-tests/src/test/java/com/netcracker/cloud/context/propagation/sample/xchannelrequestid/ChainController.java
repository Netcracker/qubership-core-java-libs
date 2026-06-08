package com.netcracker.cloud.context.propagation.sample.xchannelrequestid;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/chain")
public class ChainController {

    private static final Logger log = LoggerFactory.getLogger(ChainController.class);

    private final RestTemplate restTemplate;
    private final Environment environment;

    public ChainController(RestTemplate restTemplate, Environment environment) {
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    @GetMapping("/call")
    public ResponseEntity<Map<String, String>> call() {
        String port = environment.getRequiredProperty("local.server.port");
        log.info("Received request on /chain/call — forwarding to /chain/echo");
        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "http://localhost:" + port + "/chain/echo",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {}
        );
        log.info("Received response from /chain/echo: status={}", response.getStatusCode());
        return response;
    }

    @GetMapping("/echo")
    public Map<String, String> echo(HttpServletRequest request) {
        log.info("Received request on /chain/echo from {}", request.getRemoteAddr());
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name.toLowerCase(), request.getHeader(name));
        }
        log.info("Returning response from /chain/echo with {} headers", headers.keySet());
        return headers;
    }
}
