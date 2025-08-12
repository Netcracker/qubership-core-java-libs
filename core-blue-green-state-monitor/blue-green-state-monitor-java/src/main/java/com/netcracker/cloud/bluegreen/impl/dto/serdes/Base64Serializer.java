package com.netcracker.cloud.bluegreen.impl.dto.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Base64;

public class Base64Serializer extends StdSerializer<String> {

    public Base64Serializer() {
        super(String.class);
    }

    public Base64Serializer(Class t) {
        super(t);
    }

    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
        generator.writeString(Base64.getEncoder().encodeToString(value.getBytes()));
    }
}
