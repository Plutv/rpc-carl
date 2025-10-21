package org.example.client.serviceCenter;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.example.client.cache.ServiceCache;
import org.example.client.serviceCenter.balance.ConsistencyHashBalance;
import org.example.client.serviceCenter.zkWatcher.ZkWatcher;

import java.net.InetSocketAddress;
import java.util.List;

public class ZkServiceCenter implements ServiceCenter {
    private CuratorFramework client;

    private static final String ROOT_PATH = "MyRpc";
    private static final String RETRY = "CanRetry";

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
            String string = new ConsistencyHashBalance().balance(addressList);
            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean checkRetry(String serviceName) {
        boolean canRetry = false;
        try {
            List<String> serviceList = client.getChildren().forPath("/" + RETRY);
            for(String s : serviceList) {
                if (s.equals(serviceName)) {
                    System.out.println("[" + serviceName + "] 在白名单上，可以重试！");
                    canRetry = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return canRetry;
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
