package com.netcracker.cloud.disableapi.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DeprecatedApi {

    public Map<String, Set<String>> getDeprecatedApi(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        // find endpoints (@RequestMapping(spring), @Path(jax-rs)) annotated with @Deprecated annotation
        return handlerMethods.entrySet().stream()
                .flatMap(entry -> {
                    RequestMappingInfo requestMappingInfo = entry.getKey();
                    HandlerMethod handlerMethod = entry.getValue();
                    if (handlerMethod.getBeanType().isAnnotationPresent(Deprecated.class) ||
                            handlerMethod.getMethod().isAnnotationPresent(Deprecated.class)) {
                        Set<String> methods = requestMappingInfo.getMethodsCondition().getMethods().stream()
                                .map(rMethod -> rMethod.name().toUpperCase())
                                .collect(Collectors.toSet());
                        return Stream.of(requestMappingInfo.getDirectPaths(), requestMappingInfo.getPatternValues())
                                .flatMap(Set::stream).map(path -> new AbstractMap.SimpleEntry<>(path, methods));
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue,
                        (one, another) -> Stream.concat(one.stream(), another.stream()).collect(Collectors.toSet())));
    }
}
