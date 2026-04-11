package org.example.server.serviceRegister.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.example.server.serviceRegister.ServiceRegister;

import java.net.InetSocketAddress;

@Slf4j
public class ZKServiceRegister implements ServiceRegister {
    private static final String ROOT_PATH = "MyRpc";
    private static final String RETRY_PATH = "CanRetry";

    private final CuratorFramework client;

    public ZKServiceRegister() {
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000)
                .retryPolicy(policy)
                .namespace(ROOT_PATH)
                .build();
        this.client.start();
        log.info("zookeeper connected");
    }

    @Override
    public void register(String serviceName, InetSocketAddress serviceAddress, boolean canRetry) {
        try {
            String serviceRoot = "/" + serviceName;
            ensurePersistentPath(serviceRoot);
            String servicePath = serviceRoot + "/" + getServiceAddress(serviceAddress);
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(servicePath);
            }

            if (canRetry) {
                String retryRoot = "/" + RETRY_PATH;
                ensurePersistentPath(retryRoot);
                String retryPath = retryRoot + "/" + serviceName;
                if (client.checkExists().forPath(retryPath) == null) {
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(retryPath);
                }
            }
        } catch (Exception e) {
            log.error("Register service failed, service={}, address={}", serviceName, serviceAddress, e);
        }
    }

    @Override
    public void unregister(String serviceName, InetSocketAddress serviceAddress, boolean canRetry) {
        try {
            String servicePath = "/" + serviceName + "/" + getServiceAddress(serviceAddress);
            if (client.checkExists().forPath(servicePath) != null) {
                client.delete().forPath(servicePath);
            }

            if (canRetry) {
                String retryPath = "/" + RETRY_PATH + "/" + serviceName;
                if (client.checkExists().forPath(retryPath) != null) {
                    client.delete().forPath(retryPath);
                }
            }
        } catch (Exception e) {
            log.error("Unregister service failed, service={}, address={}", serviceName, serviceAddress, e);
        }
    }

    private void ensurePersistentPath(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        }
    }

    private String getServiceAddress(InetSocketAddress serviceAddress) {
        return serviceAddress.getHostString() + ":" + serviceAddress.getPort();
    }
}
