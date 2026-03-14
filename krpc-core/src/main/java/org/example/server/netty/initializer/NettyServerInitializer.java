package org.example.server.netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import org.example.common.serializer.mySerializer.ProtobufSerializer;
import org.example.server.netty.handler.NettyServerHandler;
import org.example.server.provider.ServiceProvider;
import org.example.common.serializer.myCode.MyDecoder;
import org.example.common.serializer.myCode.MyEncoder;
import org.example.common.serializer.mySerializer.JsonSerializer;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new IdleStateHandler(10, 20,0, TimeUnit.SECONDS));
        pipeline.addLast(new MyDecoder());
        pipeline.addLast(new MyEncoder(new ProtobufSerializer()));

        pipeline.addLast(new NettyServerHandler(serviceProvider));
    }
}
