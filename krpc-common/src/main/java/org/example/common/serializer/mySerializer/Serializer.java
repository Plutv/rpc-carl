package org.example.common.serializer.mySerializer;

import java.util.HashMap;
import java.util.Map;

public interface Serializer {
    byte[] serialize(Object obj);

    Object deserializer(byte[] bytes, int messageType);

    int getType();

    static Serializer getSerializerByCode(int code) {
        Map<Integer, Serializer> serializerMap = new HashMap<>();
        serializerMap.put(0, new ObjectSerializer());
        serializerMap.put(1, new JsonSerializer());
        serializerMap.put(2, new HessianSerializer());
        return serializerMap.get(code);
    }
}
