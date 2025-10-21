package org.example.common.serializer.mySerializer;

public interface Serializer {
    byte[] serialize(Object obj);

    Object deserializer(byte[] bytes, int messageType);

    int getType();

    static Serializer getSerializerByCode(int code) {
        switch (code) {
            case 0:
                return new ObjectSerializer();
            case 1:
                return new JsonSerializer();
            default:
                return null;
        }
    }
}
