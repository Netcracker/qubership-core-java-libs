package com.netcracker.cloud.restlegacy.restclient.error.v2;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.DEBUG_MODE_PROPERTY_NAME;

@ControllerAdvice
@RequiredArgsConstructor
@ErrorHandlerVersion2Condition
class DebugInfoFilter implements ResponseBodyAdvice<Object> {

    @Resource
    private final Environment environment;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ExceptionHandler.class) && AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof HasDebugInfo hasDebugInfo && !isDebugMode()) {
            hasDebugInfo.setDebugInfo(null);
        }
        return body;
    }

    private boolean isDebugMode() {
        return environment.getProperty(DEBUG_MODE_PROPERTY_NAME, Boolean.class, false);
    }
}
