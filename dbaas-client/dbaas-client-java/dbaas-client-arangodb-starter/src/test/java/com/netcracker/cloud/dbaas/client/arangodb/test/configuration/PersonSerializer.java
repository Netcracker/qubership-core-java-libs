package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.netcracker.cloud.dbaas.client.arangodb.entity.Person;

import java.io.IOException;

public class PersonSerializer extends JsonSerializer<Person> {
    @Override
    public void serialize(Person value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("firstName");
        gen.writeString(value.getFirstName());
        gen.writeEndObject();
    }
}
