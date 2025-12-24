package org.example.common.serializer.myCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.message.MessageType;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.common.serializer.mySerializer.Serializer;
import org.example.common.trace.TraceContext;

@Slf4j
@AllArgsConstructor
public class MyEncoder extends MessageToByteEncoder {
    private Serializer serializer;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.debug("Encoding message of type: {}", msg.getClass());

        String traceMsg = TraceContext.getTraceId() + ";" + TraceContext.getSpanId();
        byte[] traceBytes = traceMsg.getBytes();

        out.writeInt(traceBytes.length);
        out.writeBytes(traceBytes);

        if (msg instanceof RpcRequest) {
            out.writeShort(MessageType.REQUEST.getCode());
        } else if (msg instanceof RpcResponse) {
            out.writeShort(MessageType.RESPONSE.getCode());
        } else {
            log.error("unknown message type: {}", msg.getClass());
            throw new IllegalArgumentException("unknown message type: {}" + msg.getClass());
        }
        out.writeShort(serializer.getType());
        byte[] serializeBytes = serializer.serialize(msg);
        out.writeInt(serializeBytes.length);
        out.writeBytes(serializeBytes);
    }
}
