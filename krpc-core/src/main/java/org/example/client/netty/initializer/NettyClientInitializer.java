package org.example.client.netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.example.client.netty.handler.HeartbeatHandler;
import org.example.client.netty.handler.NettyClientHandler;
import org.example.common.serializer.myCode.MyDecoder;
import org.example.common.serializer.myCode.MyEncoder;
import org.example.common.serializer.mySerializer.JsonSerializer;
import org.example.common.serializer.mySerializer.ProtobufSerializer;

import java.util.concurrent.TimeUnit;

public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(0, 8,0, TimeUnit.SECONDS));
        pipeline.addLast(new HeartbeatHandler());
        pipeline.addLast(new MyDecoder());
        pipeline.addLast(new MyEncoder(new ProtobufSerializer()));

        pipeline.addLast(new NettyClientHandler());

    }
}
