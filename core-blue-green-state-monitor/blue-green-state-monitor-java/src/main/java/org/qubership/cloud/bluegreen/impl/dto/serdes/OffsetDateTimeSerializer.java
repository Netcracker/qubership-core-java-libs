package org.qubership.cloud.bluegreen.impl.dto.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.OffsetDateTime;

public class OffsetDateTimeSerializer extends StdSerializer<OffsetDateTime> {

    public OffsetDateTimeSerializer() {
        super(OffsetDateTime.class);
    }

    public OffsetDateTimeSerializer(Class t) {
        super(t);
    }

    public void serialize(OffsetDateTime date, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
        generator.writeString(date.toString());
    }
}
