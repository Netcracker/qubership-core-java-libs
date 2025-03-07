package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

class MessageParameterValueSerializer extends StdSerializer<Object> {
    public MessageParameterValueSerializer() {
        super((Class<Object>) null);
    }

    @Override
    public void serialize(Object value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (value instanceof Date) {
            jsonGenerator.writeString(((Date) value).toInstant().atZone(ZoneId.systemDefault()).format(RFC_1123_DATE_TIME));
        } else {
            serializerProvider.defaultSerializeValue(value, jsonGenerator);
        }
    }
}
