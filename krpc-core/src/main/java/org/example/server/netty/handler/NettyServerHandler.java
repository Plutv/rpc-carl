package org.example.server.netty.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.server.provider.ServiceProvider;
import org.example.server.ratelimit.RateLimit;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.trace.interceptor.ServerTraceInterceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
@AllArgsConstructor
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private ServiceProvider serviceProvider;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        if (rpcRequest == null) {
            log.error("非法请求，request为null");
            return;
        }
        ServerTraceInterceptor.beforeHandle();
        RpcResponse response = getResponse(rpcRequest);
        ServerTraceInterceptor.afterHandle(rpcRequest.getMethodName());
        channelHandlerContext.writeAndFlush(response);  // .addListener(ChannelFutureListener.CLOSE);
        // channelHandlerContext.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 只有发生异常时，才打印日志并关闭连接
        cause.printStackTrace();
        ctx.close();
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
