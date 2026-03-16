package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteFormatter implements EmbeddedValueResolverAware {

    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{[^{}]*\\}");

    private StringValueResolver stringValueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    public String processRoute(String route) {
        return processPlaceholders(route);
    }

    private String processPlaceholders(String route) {
        StringBuffer sb = new StringBuffer(route.length());
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(route);
        while (matcher.find()) {
            String property = matcher.group(0);
            String value = stringValueResolver.resolveStringValue(property);
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
