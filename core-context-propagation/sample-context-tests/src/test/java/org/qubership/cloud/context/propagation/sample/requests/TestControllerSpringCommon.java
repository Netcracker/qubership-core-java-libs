package org.qubership.cloud.context.propagation.sample.requests;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/spring/common/test")
public class TestControllerSpringCommon {

    @GetMapping
    public void get(){}
}
