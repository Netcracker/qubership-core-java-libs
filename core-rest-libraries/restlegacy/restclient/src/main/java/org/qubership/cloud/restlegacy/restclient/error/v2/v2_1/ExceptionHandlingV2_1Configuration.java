package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2_1;

@ConditionalOnProperty(name = ERROR_HANDLER_VERSION_CONDITION_PROPERTY, havingValue = VERSION_2_1)
@Configuration
@Import({ExceptionHandlerErrorCodeControllersAdvice.class})
public class ExceptionHandlingV2_1Configuration {
}
