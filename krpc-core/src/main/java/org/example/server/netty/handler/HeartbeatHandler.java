package org.example.server.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        try {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
                IdleState idleState = idleStateEvent.state();
                if (idleState == IdleState.WRITER_IDLE) {
                    log.info("写等待超过20s，关闭channel");
                } else if (idleState == IdleState.READER_IDLE) {
                    log.info("读等待超过10s，关闭channel");
                }
            }
        } catch (Exception e) {
            log.error("处理事件发生异常", e);
        }
    } 
}
