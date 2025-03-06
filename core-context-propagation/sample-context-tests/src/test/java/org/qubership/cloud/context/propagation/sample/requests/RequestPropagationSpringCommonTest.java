package org.qubership.cloud.context.propagation.sample.requests;

import org.qubership.cloud.context.propagation.spring.common.annotation.EnableSpringContextProvider;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@EnableSpringContextProvider
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestControllerSpringCommon.class)
@SpringBootTest(properties = {"cloud-core.context-propagation.url=/test_url/v111/spring/common/test"})
public class RequestPropagationSpringCommonTest {

    @Autowired
    protected WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    SpringPostAuthnContextProviderFilter filter;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilter(filter).build();
    }

    @Test
    public void testRequestPropagation() throws Exception {
        mockMvc.perform(get("/spring/common/test/requestId"))
                .andExpect(header().exists("X-Request-Id"));
    }
}
