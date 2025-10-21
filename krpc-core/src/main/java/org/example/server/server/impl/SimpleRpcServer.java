package org.example.server.server.impl;

import org.example.server.provider.ServiceProvider;
import org.example.server.server.RpcServer;
import org.example.server.work.WorkThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleRpcServer implements RpcServer {
    private ServiceProvider serviceProvider;

    @Override
    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("服务器启动");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new WorkThread(socket, serviceProvider)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {

    }
}
