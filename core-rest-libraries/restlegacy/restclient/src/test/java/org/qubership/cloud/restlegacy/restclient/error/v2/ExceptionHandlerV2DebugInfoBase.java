package org.qubership.cloud.restlegacy.restclient.error.v2;

import lombok.Getter;
import lombok.Setter;
import org.hamcrest.Matcher;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.THROW_ANY_EXCEPTION_METHOD;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.DEBUG_MODE_PROPERTY_NAME;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// @Ignore is added to prevent test failing if you will execute tests directly on this class from IDE.
// It is norm behaviour, because IDE test runner starts all tests annotated by @Test.
// But maven surefire plugin (is used to execute test during maven build) will not start test for this class never, because class name does not contain 'Test'.
// See details here http://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html
@Ignore
public class ExceptionHandlerV2DebugInfoBase {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JacksonTester<HasDebugInfoImpl> debugInfoTester;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @After
    public void clear() {
        System.clearProperty(DEBUG_MODE_PROPERTY_NAME);
    }

    @Test
    public void sendDebugInfoIfDebugModeIsOnDirectly() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "true");

        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(notNullValue(DebugInfo.class)));
    }

    @Test
    public void dontSendDebugInfoByDefault() throws Exception {
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }

    @Test
    public void dontSendDebugInfoIfDebugModeIsOffDirectly() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "false");

        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }

    @Test
    public void weCanChangeDebugModeValueWithoutRestartingEnvironment() throws Exception {
        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "true");
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(notNullValue(DebugInfo.class)));

        System.setProperty(DEBUG_MODE_PROPERTY_NAME, "false");
        mockMvc.perform(get(THROW_ANY_EXCEPTION_METHOD)).andExpect(bodyHasDebugInfo(nullValue(DebugInfo.class)));
    }


    private ResultMatcher bodyHasDebugInfo(Matcher<DebugInfo> matcher) {
        return result -> {
            String contentAsString = result.getResponse().getContentAsString();
            DebugInfo debugInfo = debugInfoTester.parseObject(contentAsString).getDebugInfo();

            Assert.assertThat(debugInfo, matcher);
        };
    }

    public static class HasDebugInfoImpl implements HasDebugInfo<DebugInfo> {
        @Getter
        @Setter
        public DebugInfo debugInfo;
    }
}