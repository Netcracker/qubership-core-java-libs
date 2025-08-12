package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import com.netcracker.cloud.restlegacy.restclient.error.TestExceptionHandlingConfiguration;
import com.netcracker.cloud.restlegacy.restclient.error.v2.ExceptionHandlerV2DebugInfoBase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2_1;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestExceptionHandlingConfiguration.class},
        properties = {ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "=" + VERSION_2_1})
@AutoConfigureJsonTesters
public class ExceptionHandlerV2_1DebugInfoTest extends ExceptionHandlerV2DebugInfoBase {
}
