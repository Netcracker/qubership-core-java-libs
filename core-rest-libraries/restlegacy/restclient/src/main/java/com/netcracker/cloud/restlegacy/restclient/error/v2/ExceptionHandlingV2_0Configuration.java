package com.netcracker.cloud.restlegacy.restclient.error.v2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2;

@ConditionalOnProperty(name = ERROR_HANDLER_VERSION_CONDITION_PROPERTY, havingValue = VERSION_2)
@Configuration
@Import({ExceptionHandlerControllersAdvice.class})
public class ExceptionHandlingV2_0Configuration {
}
