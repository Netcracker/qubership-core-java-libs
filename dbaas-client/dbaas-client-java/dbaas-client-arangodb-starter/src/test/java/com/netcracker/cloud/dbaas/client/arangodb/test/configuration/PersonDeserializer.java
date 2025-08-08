package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.netcracker.cloud.dbaas.client.arangodb.entity.Person;

import java.io.IOException;

class PersonDeserializer extends JsonDeserializer<Person> {
    @Override
    public Person deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        Person person = new Person();
        JsonNode rootNode = parser.getCodec().readTree(parser);
        JsonNode nameNode = rootNode.get("name");
        if (nameNode != null && nameNode.isTextual()) {
            person.setFirstName(nameNode.asText());
        }
        return person;
    }
}
