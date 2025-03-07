package org.qubership.cloud.bluegreen.impl.dto.serdes;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Base64;

public class Base64Deserializer extends StdDeserializer<String> {

    public Base64Deserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
        return new String(Base64.getDecoder().decode(parser.readValueAs(String.class)));
    }

    public Base64Deserializer(Class t) {
        super(t);
    }

    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
        generator.writeString(Base64.getEncoder().encodeToString(value.getBytes()));
    }
}
