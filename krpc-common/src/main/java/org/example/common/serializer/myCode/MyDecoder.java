package org.example.common.serializer.myCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.example.common.message.MessageType;
import org.example.common.serializer.mySerializer.Serializer;
import org.example.common.trace.TraceContext;

import java.util.List;

public class MyDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 12) {
            return;
        }

        int traceLength = in.readInt();
        byte[] traceBytes = new byte[traceLength];
        in.readBytes(traceBytes);
        serializeTraceMsg(traceBytes);

        short messageType = in.readShort();
        if (messageType != MessageType.REQUEST.getCode() && messageType != MessageType.RESPONSE.getCode()) {
            System.out.println("not support this message type");
        }
        short serializerType = in.readShort();
        Serializer serializer = Serializer.getSerializerByCode(serializerType);
        if (serializer == null) {
            throw new RuntimeException("not exists corresponding serializer!");
        }
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        Object deserialize = serializer.deserializer(bytes, messageType);
        out.add(deserialize);
    }

    private void serializeTraceMsg(byte[] bytes) {
        String traceMsg = new String(bytes);
        String[] msgs = traceMsg.split(";");
        if(!msgs[0].equals("")) TraceContext.setTraceId(msgs[0]);
        if(!msgs[1].equals("")) TraceContext.setParentSpanId(msgs[1]);
    }
}
