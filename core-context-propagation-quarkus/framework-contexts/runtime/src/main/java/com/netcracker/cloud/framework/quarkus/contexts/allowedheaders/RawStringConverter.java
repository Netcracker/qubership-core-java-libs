package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import org.eclipse.microprofile.config.spi.Converter;

public class RawStringConverter implements Converter<String> {
    @Override
    public String convert(String value) {
        return value;
    }
}