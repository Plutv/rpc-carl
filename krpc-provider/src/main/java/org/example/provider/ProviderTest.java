package org.example.provider;

import org.example.KRpcApplication;
import org.example.server.provider.ServiceProvider;
import org.example.server.server.RpcServer;
import org.example.server.server.impl.NettyRpcServer;
import org.example.service.UserService;
import org.example.provider.impl.UserServiceImpl;

public class ProviderTest {
    public static void main(String[] args) {
        KRpcApplication.initialize();
        UserService userService = new UserServiceImpl();
        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 9999);
        serviceProvider.provideServiceInterface(userService, true);
        RpcServer rpcServer = new NettyRpcServer(serviceProvider);
        rpcServer.start(9999);
    }
}
