package org.example.client.rpcClient.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.example.client.serviceCenter.ServiceCenter;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.client.netty.initializer.NettyClientInitializer;
import org.example.client.rpcClient.RpcClient;

import java.net.InetSocketAddress;

public class NettyRpcClient implements RpcClient {
    private String host;
    private int port;
    public static final Bootstrap bootstrap;
    public static final EventLoopGroup eventLoopGroup;

    private ServiceCenter serviceCenter;

    public NettyRpcClient(ServiceCenter serviceCenter) throws InterruptedException{
        this.serviceCenter = serviceCenter;
    }

    public NettyRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }

    @Override
    public RpcResponse sendRequest(RpcRequest rpcRequest) {
        try {
            InetSocketAddress address = serviceCenter.serviceDiscovery(rpcRequest.getInterfaceName());
            String host = address.getHostName();
            int port = address.getPort();
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            Channel channel = channelFuture.channel();

            channel.writeAndFlush(rpcRequest);

            channel.closeFuture().sync();
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("RpcResponse");
            RpcResponse response = channel.attr(key).get();
            System.out.println(response);
            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
