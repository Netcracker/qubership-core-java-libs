package org.qubership.cloud.restlegacy.restclient.error.v2;

import org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestExceptionHandlingConfiguration.class},
        properties = {ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "=" + VERSION_2})
@AutoConfigureJsonTesters
public class ExceptionHandlerV2_0DebugInfoTest extends ExceptionHandlerV2DebugInfoBase {
}
