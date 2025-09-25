package org.example.Server.server.impl;

import org.example.Server.provider.ServiceProvider;
import org.example.Server.server.RpcServer;
import org.example.Server.work.WorkThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolRpcServer implements RpcServer {
    private final ThreadPoolExecutor threadPool;
    private ServiceProvider serviceProvider;

    public ThreadPoolRpcServer(ServiceProvider serviceProvider) {
        threadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                1000, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        this.serviceProvider = serviceProvider;
    }

    public ThreadPoolRpcServer(ServiceProvider serviceProvider, int corePoolSize, int maximumPoolSize, long keepAliveTime,
                               TimeUnit unit, BlockingDeque<Runnable> workQueue) {
        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void start(int port) {
        System.out.println("服务器启动");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(new WorkThread(socket, serviceProvider));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {

    }
}
