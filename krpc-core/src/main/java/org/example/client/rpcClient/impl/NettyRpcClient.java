package org.example.client.rpcClient.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.example.client.netty.initializer.NettyClientInitializer;
import org.example.client.rpcClient.RpcClient;
import org.example.client.serviceCenter.ServiceCenter;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.common.trace.TraceContext;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.Map;

@Slf4j
public class NettyRpcClient implements RpcClient {
    private String host;
    private int port;
    public static final Bootstrap bootstrap;
    public static final EventLoopGroup eventLoopGroup;

    private ServiceCenter serviceCenter;

    public NettyRpcClient(ServiceCenter serviceCenter) throws InterruptedException {
        this.serviceCenter = serviceCenter;
    }

    public NettyRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    class MDCChannelHandler extends ChannelOutboundHandlerAdapter {
        private final Map<String, String> mdcContext;

        public MDCChannelHandler(Map<String, String> mdcContext) {
            this.mdcContext = mdcContext;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            super.write(ctx, msg, promise);
        }

//        @Override
//        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//            if (mdcContext != null) {
//                MDC.setContextMap(mdcContext);
//            }
//            super.channelActive(ctx);
//        }
//
//        @Override
//        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//            MDC.clear();
//            super.channelInactive(ctx);
//        }
    }

    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
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

    class MDCChannelHandler extends ChannelOutboundHandlerAdapter {
        private final Map<String, String> mdcContext;

        public MDCChannelHandler(Map<String, String> mdcContext) {
            this.mdcContext = mdcContext;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        Map<String, String> mdcContext = TraceContext.getCopy();
        // Resolve the target endpoint from the registry or the fixed host/port.
        InetSocketAddress address;
        if (serviceCenter != null) {
            address = serviceCenter.serviceDiscovery(request.getInterfaceName());
        } else if (host != null && !host.isEmpty() && port > 0) {
            address = new InetSocketAddress(host, port);
        } else {
            log.error("No available serviceCenter or fixed address for request: {}", request.getInterfaceName());
            return RpcResponse.fail("No available serviceCenter or fixed address");
        }

        if (address == null) {
            log.error("Service discovery returned null address for request: {}", request.getInterfaceName());
            return RpcResponse.fail("Service discovery returned null address");
        }

        String host = address.getHostName();
        int port = address.getPort();
        try {
            // Connect to the remote provider.
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            Channel channel = channelFuture.channel();
            channel.pipeline().addLast(new MDCChannelHandler(mdcContext));

            // Send the RPC request.
            channel.writeAndFlush(request);

            // Wait until the channel is closed so the response can be attached.
            channel.closeFuture().sync();

            // The inbound handler stores the RpcResponse on the channel attribute.
            // Read it back after the channel is closed.
            // If later you want non-blocking behavior, replace this flow with channelFuture listeners.
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("RpcResponse");
            RpcResponse response = channel.attr(key).get();

            if (response == null) {
                log.error("rpc response is null, request may have failed or timed out");
                return RpcResponse.fail("rpc response is null");
            }

            log.info("rpc response received: {}", response);
            return response;
        } catch (InterruptedException e) {
            log.error("rpc request was interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("rpc request failed: {}", e.getMessage(), e);
        } finally {
            // Keep the shared Netty client alive instead of shutting it down per request.
            // shutdown();
        }
        return RpcResponse.fail("rpc request failed");
    }

    // Shut down the shared Netty event loop when the client is no longer needed.
    private void shutdown() {
        try {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully().sync();
            }
        } catch (InterruptedException e) {
            log.error("failed to shut down Netty event loop: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
