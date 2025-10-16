package org.example.Client.serviceCenter;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.example.Client.cache.ServiceCache;
import org.example.Client.serviceCenter.zkWatcher.ZkWatcher;

import java.net.InetSocketAddress;
import java.util.List;

public class ZkServiceCenter implements ServiceCenter {
    private CuratorFramework client;

    private static final String ROOT_PATH = "MyRpc";

    private ServiceCache serviceCache;

    public ZkServiceCenter() throws InterruptedException{
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181").
                sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");
        this.serviceCache = new ServiceCache();
        ZkWatcher zkWatcher = new ZkWatcher(client, serviceCache);
        zkWatcher.watchToUpdate(ROOT_PATH);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            List<String> addressList = serviceCache.getServiceFromCache(serviceName);
            if (addressList == null) {
                addressList = client.getChildren().forPath("/" + serviceName);
            }
            String string = addressList.get(0);
            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" + serverAddress.getPort();
    }

    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }
}
