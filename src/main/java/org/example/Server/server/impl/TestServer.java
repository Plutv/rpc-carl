package org.example.Server.server.impl;

import org.example.Server.provider.ServiceProvider;
import org.example.Server.server.RpcServer;
import org.example.common.service.UserService;
import org.example.common.service.impl.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        RpcServer rpcServer = new NettyRpcServer(serviceProvider);
        rpcServer.start(9999);
    }
}
