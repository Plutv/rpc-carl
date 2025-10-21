package org.example.server.server.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.server.netty.initializer.NettyServerInitializer;
import org.example.server.provider.ServiceProvider;
import org.example.server.server.RpcServer;

@Slf4j
@AllArgsConstructor
public class NettyRpcServer implements RpcServer {
    private ServiceProvider serviceProvider;

    private ChannelFuture channelFuture;

    public NettyRpcServer(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void start(int port) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        System.out.println("netty 服务器启动");
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer(serviceProvider));
            channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
        if (channelFuture != null) {
            try {
                channelFuture.channel().close().sync();
                log.info("Netty服务主通道已关闭");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("关闭Netty服务主通道时中断", e.getMessage(), e);
            }
        } else {
            log.warn("Netty服务尚未启动，无法关闭！");
        }
    }

    private void shutdown(NioEventLoopGroup bossGroup, NioEventLoopGroup workGroup) {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
