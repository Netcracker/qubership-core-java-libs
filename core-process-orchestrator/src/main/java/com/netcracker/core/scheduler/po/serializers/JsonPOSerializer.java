package com.netcracker.core.scheduler.po.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import lombok.SneakyThrows;

public class JsonPOSerializer implements ExtendedSerializer {
    public final ObjectMapper mapper;

    public JsonPOSerializer() {
        ClassLoader classLoader = ProcessInstanceImpl.class.getClassLoader();
        if ((classLoader.getClass().getName()).equals("io.quarkus.bootstrap.classloading.QuarkusClassLoader"))
            classLoader = Thread.currentThread().getContextClassLoader();
        this.mapper = new ObjectMapper();
        mapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
        mapper.registerModule(new JavaTimeModule());
        mapper.setTypeFactory(mapper.getTypeFactory().withClassLoader(classLoader));

    }

    @SneakyThrows
    @Override
    public byte[] serialize(Object data) {
        return mapper.writeValueAsBytes(data);
    }

    @SneakyThrows
    @Override
    public <T> T deserialize(Class<T> clazz, byte[] serializedData) {
        return mapper.readValue(serializedData, clazz);
    }

    @SneakyThrows
    @Override
    public <T> T deserialize(TypeReference<T> clazz, byte[] serializedData) {
        return mapper.readValue(serializedData, clazz);
    }
}
