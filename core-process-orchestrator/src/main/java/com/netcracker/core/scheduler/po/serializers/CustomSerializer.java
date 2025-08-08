package com.netcracker.core.scheduler.po.serializers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.kagkarlsson.scheduler.exceptions.SerializationException;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;


import java.io.*;
import java.lang.reflect.ParameterizedType;

public class CustomSerializer implements ExtendedSerializer {
    public byte[] serialize(Object data) {
        if (data == null) return null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(data);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object", e);
        }
    }

    public <T> T deserialize(Class<T> clazz, byte[] serializedData) {
        if (serializedData == null) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedData)) {
            return deserialize(bis);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize object", e);
        }
    }

    public <T> T deserialize(final InputStream inputStream) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = ProcessInstanceImpl.class.getClassLoader();
        if ((classLoader.getClass().getName()).equals("io.quarkus.bootstrap.classloading.QuarkusClassLoader"))
            classLoader = Thread.currentThread().getContextClassLoader();
        try (ObjectInputStream in = new ClassLoaderObjectInputStream(classLoader, inputStream)) {
            @SuppressWarnings("unchecked") final T obj = (T) in.readObject();
            return obj;
        }
    }

    @Override
    public <T> T deserialize(TypeReference<T> clazz, byte[] serializedData) {
        ParameterizedType type = (ParameterizedType) clazz.getType();


        return (T) deserialize((Class)type.getRawType(), serializedData);
    }
}
