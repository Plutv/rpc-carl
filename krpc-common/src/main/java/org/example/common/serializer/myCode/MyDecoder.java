package org.example.common.serializer.myCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.example.common.message.MessageType;
import org.example.common.serializer.mySerializer.Serializer;
import org.example.common.trace.TraceContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MyDecoder extends ByteToMessageDecoder {
    private static final short MAGIC = (short) 0xCAFE;
    private static final byte VERSION = 1;
    private static final int MAGIC_BYTES = 2;
    private static final int VERSION_BYTES = 1;
    private static final int INT_BYTES = 4;
    private static final int SHORT_BYTES = 2;
    private static final int MIN_FIXED_LENGTH = SHORT_BYTES + SHORT_BYTES + INT_BYTES;
    private static final int MIN_FRAME_HEADER = MAGIC_BYTES + VERSION_BYTES + INT_BYTES + MIN_FIXED_LENGTH;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MIN_FRAME_HEADER) {
            return;
        }

        in.markReaderIndex();

        short magic = in.readShort();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid magic: " + magic);
        }

        byte version = in.readByte();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported protocol version: " + version);
        }

        int traceLength = in.readInt();
        if (traceLength < 0) {
            throw new IllegalArgumentException("traceLength is negative");
        }

        if (in.readableBytes() < traceLength + MIN_FIXED_LENGTH) {
            in.resetReaderIndex();
            return;
        }

        byte[] traceBytes = new byte[traceLength];
        in.readBytes(traceBytes);
        deserializeTraceMsg(traceBytes);

        short messageType = in.readShort();
        if (messageType != MessageType.REQUEST.getCode() && messageType != MessageType.RESPONSE.getCode()) {
            throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }

        short serializerType = in.readShort();
        Serializer serializer = Serializer.getSerializerByCode(serializerType);
        if (serializer == null) {
            throw new IllegalArgumentException("No serializer for type: " + serializerType);
        }

        int bodyLength = in.readInt();
        if (bodyLength < 0) {
            throw new IllegalArgumentException("bodyLength is negative");
        }

        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes);
        Object deserialize = serializer.deserializer(bodyBytes, messageType);
        out.add(deserialize);
    }

    private void deserializeTraceMsg(byte[] bytes) {
        String traceMsg = new String(bytes, StandardCharsets.UTF_8);
        String[] msgs = traceMsg.split(";", -1);
        if (msgs.length > 0 && !msgs[0].isEmpty()) {
            TraceContext.setTraceId(msgs[0]);
        }
        if (msgs.length > 1 && !msgs[1].isEmpty()) {
            TraceContext.setParentSpanId(msgs[1]);
        }
    }
}
