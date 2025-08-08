package com.netcracker.core.scheduler.po.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import lombok.SneakyThrows;

import java.util.Iterator;

public class DataContextDeserializer extends JsonDeserializer<DataContext> {
    private final ClassLoader classLoader;

    public DataContextDeserializer() {
        if ((ProcessInstanceImpl.class.getClassLoader().getClass().getName()).equals("io.quarkus.bootstrap.classloading.QuarkusClassLoader"))
            classLoader = Thread.currentThread().getContextClassLoader();
        else classLoader = ProcessInstanceImpl.class.getClassLoader();
    }

    @SneakyThrows
    @Override
    public DataContext deserialize(JsonParser jp, DeserializationContext deserializationContext) {
        JsonNode node = jp.getCodec().readTree(jp);
        DataContext context = new DataContext(node.get("id").toString());
        context.setVersion(node.get("version").asInt());
        JsonNode customData = node.get("customData");
        for (Iterator<String> it = customData.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            JsonNode data = customData.get(key);
            String clazz = data.get("class").textValue();
            Object o = deserializationContext.readTreeAsValue(data.get("value"), classLoader.loadClass(clazz));
            context.put(key, o);
        }

        return context;
    }
}
