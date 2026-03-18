package com.netcracker.cloud.restlegacy.restclient.error.v2;

import lombok.Getter;
import lombok.Setter;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static com.netcracker.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.THROW_ANY_EXCEPTION_METHOD;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.DEBUG_MODE_PROPERTY_NAME;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class ExceptionHandlerV2DebugInfoBase {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JacksonTester<HasDebugInfoImpl> debugInfoTester;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void clear() {
        System.clearProperty(DEBUG_MODE_PROPERTY_NAME);
    }

    @Test
    void sendDebugInfoIfDebugModeIsOnDirectly() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "true");

        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(notNullValue(DebugInfo.class)));
    }

    @Test
    void dontSendDebugInfoByDefault() throws Exception {
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }

    @Test
    void dontSendDebugInfoIfDebugModeIsOffDirectly() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "false");

        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }

    @Test
    void weCanChangeDebugModeValueWithoutRestartingEnvironment() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "true");
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(notNullValue(DebugInfo.class)));

        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "false");
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }


    private ResultMatcher bodyHasDebugInfo(Matcher<DebugInfo> matcher) {
        return result -> {
            String contentAsString = result.getResponse().getContentAsString();
            DebugInfo debugInfo = debugInfoTester.parseObject(contentAsString).getDebugInfo();

            assertThat(debugInfo, matcher);
        };
    }

    public static class HasDebugInfoImpl implements HasDebugInfo<DebugInfo> {
        @Getter
        @Setter
        public DebugInfo debugInfo;
    }
}
