package org.example.client.rpcClient.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.example.client.serviceCenter.ServiceCenter;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.client.netty.initializer.NettyClientInitializer;
import org.example.client.rpcClient.RpcClient;

import java.net.InetSocketAddress;

@Slf4j
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

    // @Override
    // public RpcResponse sendRequest(RpcRequest rpcRequest) {
    //     try {
    //         InetSocketAddress address = serviceCenter.serviceDiscovery(rpcRequest.getInterfaceName());
    //         String host = address.getHostName();
    //         int port = address.getPort();
    //         ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
    //         Channel channel = channelFuture.channel();
    //
    //         channel.writeAndFlush(rpcRequest);
    //
    //         channel.closeFuture().sync();
    //         AttributeKey<RpcResponse> key = AttributeKey.valueOf("RpcResponse");
    //         RpcResponse response = channel.attr(key).get();
    //         System.out.println(response);
    //         return response;
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //         return null;
    //     }
    // }

    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        //从注册中心获取host,post
        InetSocketAddress address = serviceCenter.serviceDiscovery(request.getInterfaceName());
        if (address == null) {
            log.error("服务发现失败，返回的地址为 null");
            return RpcResponse.fail("服务发现失败，地址为 null");
        }
        String host = address.getHostName();
        int port = address.getPort();
        try {
            // 连接到远程服务
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            Channel channel = channelFuture.channel();
            // 发送数据
            channel.writeAndFlush(request);
            //sync()堵塞获取结果
            channel.closeFuture().sync();
            // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            // AttributeKey是，线程隔离的，不会由线程安全问题。
            // 当前场景下选择堵塞获取结果
            // 其它场景也可以选择添加监听器的方式来异步获取结果 channelFuture.addListener...
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("RPCResponse");
            RpcResponse response = channel.attr(key).get();

            if (response == null) {
                log.error("服务响应为空，可能是请求失败或超时");
                return RpcResponse.fail("服务响应为空");
            }

            log.info("收到响应: {}", response);
            return response;
        } catch (InterruptedException e) {
            log.error("请求被中断，发送请求失败: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("发送请求时发生异常: {}", e.getMessage(), e);
        } finally {
            // 连接断开后，优雅地关闭 Netty 资源
            shutdown();
        }
        return RpcResponse.fail("请求失败");
    }

    // 优雅关闭 Netty 资源
    private void shutdown() {
        try {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully().sync();
            }
        } catch (InterruptedException e) {
            log.error("关闭 Netty 资源时发生异常: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
