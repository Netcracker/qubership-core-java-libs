package org.qubership.cloud.restlegacy.restclient.error.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import jakarta.annotation.Resource;

import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.DEBUG_MODE_PROPERTY_NAME;

@ControllerAdvice
@RequiredArgsConstructor
@ErrorHandlerVersion2Condition
class DebugInfoFilter extends AbstractMappingJacksonResponseBodyAdvice {
    @Resource
    private final Environment environment;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ExceptionHandler.class) && super.supports(returnType, converterType);
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        final Object value = bodyContainer.getValue();
        if (!(value instanceof HasDebugInfo) || isDebugMode()) {
            return;
        }

        ((HasDebugInfo) value).setDebugInfo(null);
    }

    private boolean isDebugMode() {
        final String debugMode = environment.getProperty(DEBUG_MODE_PROPERTY_NAME);
        return debugMode != null && debugMode.equalsIgnoreCase("true");
    }
}
