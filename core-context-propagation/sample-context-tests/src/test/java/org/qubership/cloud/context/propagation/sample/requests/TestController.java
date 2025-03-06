package org.qubership.cloud.context.propagation.sample.requests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping(path = "/test")
public class TestController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping
    public void propagateContexts() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/chain_request", HttpMethod.GET, null,
                String.class);
    }
}
