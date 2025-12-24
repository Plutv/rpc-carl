package org.example.common.serializer.mySerializer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HessianSerializerTest {
    private final HessianSerializer serializer = new HessianSerializer();

    @Test
    void testSerializeAndDeserialize() {
        String original = "Hello, Hessian!";

        byte[] serialized = serializer.serialize(original);
        assertNotNull(serialized, "序列化结果不应为 null");

        Object deserialized = serializer.deserializer(serialized, 3);
        assertNotNull(deserialized, "反序列化结果不应为 null");

        assertEquals(original, deserialized, "反序列化结果应与原始对象相同");
    }
}
