package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingConfiguration;
import org.qubership.cloud.restlegacy.restclient.error.v2.ExceptionHandlerV2DebugInfoBase;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2_1;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestExceptionHandlingConfiguration.class},
        properties = {ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "=" + VERSION_2_1})
@AutoConfigureJsonTesters
public class ExceptionHandlerV2_1DebugInfoTest extends ExceptionHandlerV2DebugInfoBase {
}
