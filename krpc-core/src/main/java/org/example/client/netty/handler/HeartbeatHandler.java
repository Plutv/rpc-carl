package org.example.client.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.example.common.message.RpcRequest;

@Slf4j
public class HeartbeatHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            if (idleState == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(RpcRequest.heartBeat());
                log.info("写等待超过8s，发送心跳包");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    } 
}
