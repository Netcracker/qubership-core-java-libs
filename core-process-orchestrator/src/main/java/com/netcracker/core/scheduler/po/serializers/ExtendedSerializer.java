package com.netcracker.core.scheduler.po.serializers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.kagkarlsson.scheduler.serializer.Serializer;

public interface ExtendedSerializer extends Serializer {
    <T> T deserialize(TypeReference<T> clazz, byte[] serializedData);


}
