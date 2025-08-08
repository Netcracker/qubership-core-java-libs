package com.netcracker.cloud.context.propagation.sample.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.spring.common.annotation.EnableSpringContextProvider;
import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@EnableSpringContextProvider
@ContextConfiguration(classes = TestControllerSpringCommon.class)
@SpringBootTest(properties = {"cloud-core.context-propagation.url=/test_url/v111/spring/common/test"})
class RequestPropagationSpringCommonTest {

    @Autowired
    protected WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    SpringPostAuthnContextProviderFilter filter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilter(filter).build();
    }

    @Test
    void testRequestPropagation() throws Exception {
        mockMvc.perform(get("/spring/common/test/requestId"))
                .andExpect(header().exists("X-Request-Id"));
    }
}
