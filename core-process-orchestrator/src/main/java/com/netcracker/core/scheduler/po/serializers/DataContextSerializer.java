package com.netcracker.core.scheduler.po.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.netcracker.core.scheduler.po.DataContext;

import java.io.IOException;
import java.util.Map;

public class DataContextSerializer extends JsonSerializer<DataContext> {
    @Override
    public void serialize(DataContext context, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", context.getId());
        jsonGenerator.writeNumberField("version", context.getVersion());
        jsonGenerator.writeFieldName("customData");

        jsonGenerator.writeStartObject();

        for (Map.Entry<String, Object> entry : context.entrySet()) {
                jsonGenerator.writeFieldName(entry.getKey());
                jsonGenerator.writeStartObject();

                jsonGenerator.writeStringField("class", entry.getValue().getClass().getName());
                jsonGenerator.writeObjectField("value", entry.getValue());

                jsonGenerator.writeEndObject();

        }

        jsonGenerator.writeEndObject();

        jsonGenerator.writeEndObject();

    }

}
