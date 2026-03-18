package com.netcracker.cloud.log.manager.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
class LoggingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetLoggers() throws Exception {
        mockMvc.perform(get("/api/logging/v1/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }
}
