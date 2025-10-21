package org.example.server.serviceRegister.impl;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.example.server.serviceRegister.ServiceRegister;

import java.net.InetSocketAddress;

public class ZKServiceRegister implements ServiceRegister {
    private CuratorFramework client;

    private static final String ROOT_PATH = "MyRpc";
    private static final String RETRY = "CanRetry";

    public ZKServiceRegister() {
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper connected!");
    }

    @Override
    public void register(String serviceName, InetSocketAddress serviceAddress, boolean canRetry) {
        try {
            if (client.checkExists().forPath("/" + serviceName) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }
            String path = "/" + serviceName + "/" + getServiceAddress(serviceAddress);
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
            if (canRetry) {
                path = "/" + RETRY + "/" + serviceName;
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
            }
        } catch (Exception e) {
            System.out.println("service existed!");
        }
    }

    private String getServiceAddress(InetSocketAddress serviceAddress) {
        return serviceAddress.getHostName() + ":" + serviceAddress.getPort();
    }

    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }
}
