package com.netcracker.cloud.restlegacy.restclient.error.v2;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ErrorHandlerVersion2Condition
@Configuration
@Import({RestClientExceptionPropagatorV2.class, DebugInfoFilter.class})
public class ExceptionHandlingV2MainConfiguration {
}
