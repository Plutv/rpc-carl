package org.example.common.serializer.mySerializer;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import org.example.common.exception.SerializeException;

import javax.sql.rowset.serial.SerialException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianSerializer implements Serializer{
    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializeException("序列化失败！");
        }
    }

    @Override
    public Object deserializer(byte[] bytes, int messageType) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            return hessianInput.readObject();
        } catch (IOException e) {
            throw new SerializeException("反序列化失败！");
        }
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public String toString() {
        return "Hessian";
    }
}
