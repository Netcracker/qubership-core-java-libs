/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netcracker.cloud.configserver.common.sample;

import com.netcracker.cloud.configserver.common.configuration.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = {Application.class, TestConfiguration.class},
        // Normally spring.cloud.config.enabled:true is the default but since we have the
        // config
        // server on the classpath we need to set it explicitly
        properties = {
                "spring.cloud.config.enabled:true",
                "management.security.enabled=false",
                "management.endpoints.web.exposure.include=*",
                "cloud.microservice.name=my-test-app",
                TestConfiguration.REFRESH_EVENT_AMOUNT_PROP_KEY + "=1",
                TestConfiguration.CONTEXT_REFRESH_AMOUNT_PROP_KEY + "=1"
        },
        webEnvironment = RANDOM_PORT)
@EnableAutoConfiguration(exclude = {ConfigClientAutoConfiguration.class})
public class ApplicationTests {


    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        String res = new TestRestTemplate().getForObject(
                "http://localhost:" + this.port + "/test", String.class);
        assertEquals("test_value", res);
    }
}
