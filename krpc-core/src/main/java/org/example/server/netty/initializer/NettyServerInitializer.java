package org.example.server.netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.AllArgsConstructor;
import org.example.server.netty.handler.NettyServerHandler;
import org.example.server.provider.ServiceProvider;
import org.example.common.serializer.myCode.MyDecoder;
import org.example.common.serializer.myCode.MyEncoder;
import org.example.common.serializer.mySerializer.JsonSerializer;

@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyDecoder());
        pipeline.addLast(new MyEncoder(new JsonSerializer()));

        pipeline.addLast(new NettyServerHandler(serviceProvider));
    }
}
