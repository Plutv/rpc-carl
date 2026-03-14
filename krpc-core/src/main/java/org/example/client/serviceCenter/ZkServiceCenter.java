package org.example.client.serviceCenter;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.example.client.cache.ServiceCache;
import org.example.client.serviceCenter.balance.ConsistencyHashBalance;
import org.example.client.serviceCenter.zkWatcher.ServiceChangeListener;
import org.example.client.serviceCenter.zkWatcher.ZkWatcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceCenter implements ServiceCenter {
    private CuratorFramework client;

    private static final String ROOT_PATH = "MyRpc";
    private static final String RETRY = "CanRetry";

    private final ServiceCache serviceCache;

    /**
     * 每个 serviceName 维护一套一致性哈希环，避免不同服务的节点混到同一个环里。
     */
    private final Map<String, ConsistencyHashBalance> balanceMap = new ConcurrentHashMap<>();

    public ZkServiceCenter() throws InterruptedException {
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181").
                sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");

        this.serviceCache = new ServiceCache();

        // watcher 只负责通知变化；由 ZkServiceCenter 统一协调更新 cache + hash ring
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onAdd(String serviceName, String address) {
                serviceCache.addServiceToCache(serviceName, address);
                balanceMap.computeIfAbsent(serviceName, k -> new ConsistencyHashBalance()).addNode(address);
            }

            @Override
            public void onRemove(String serviceName, String address) {
                serviceCache.delete(serviceName, address);
                ConsistencyHashBalance balance = balanceMap.get(serviceName);
                if (balance != null) {
                    balance.delNode(address);
                }
            }
        };

        ZkWatcher zkWatcher = new ZkWatcher(client, listener);
        zkWatcher.watchToUpdate(ROOT_PATH);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            List<String> addressList = serviceCache.getServiceFromCache(serviceName);
            if (addressList.isEmpty()) {
                addressList = client.getChildren().forPath("/" + serviceName);
                for (String address : addressList) {
                    serviceCache.addServiceToCache(serviceName, address);
                }
                // 初始化该服务对应的哈希环（首次发现时）
                ConsistencyHashBalance balance = balanceMap.computeIfAbsent(serviceName, k -> new ConsistencyHashBalance());
                for (String address : addressList) {
                    balance.addNode(address);
                }
            }

            ConsistencyHashBalance balance = balanceMap.computeIfAbsent(serviceName, k -> new ConsistencyHashBalance());
            String picked = balance.balance(addressList);
            return parseAddress(picked);
        } catch (Exception e) {
            log.error("服务发现失败，服务名{}", serviceName, e);
        }
        return null;
    }

    @Override
    public boolean checkRetry(String serviceName) {
        boolean canRetry = false;
        try {
            List<String> serviceList = client.getChildren().forPath("/" + RETRY);
            for (String s : serviceList) {
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
