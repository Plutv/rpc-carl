package org.example.server.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import org.example.server.provider.ServiceProvider;
import org.example.server.ratelimit.RateLimit;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@AllArgsConstructor
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private ServiceProvider serviceProvider;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        RpcResponse response = getResponse(rpcRequest);
        channelHandlerContext.writeAndFlush(response);
        channelHandlerContext.close();
    }

    private RpcResponse getResponse(RpcRequest rpcRequest) {
        String interfaceName = rpcRequest.getInterfaceName();
        RateLimit rateLimit = serviceProvider.getRateLimitProvider().getRateLimit(interfaceName);
        if (!rateLimit.getToken()) {
            System.out.println("server current-limiting");
            return RpcResponse.fail();
        }
        Object service = serviceProvider.getService(rpcRequest.getInterfaceName());
        String methodName = rpcRequest.getMethodName();
        Method method = null;
        try {
            method = service.getClass().getMethod(methodName, rpcRequest.getParamsType());
            Object invoke = method.invoke(service, rpcRequest.getParams());
            return RpcResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RpcResponse.fail();
        }
    }
}
