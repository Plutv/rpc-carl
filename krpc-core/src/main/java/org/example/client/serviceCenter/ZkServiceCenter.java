package org.example.client.serviceCenter;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.example.KRpcApplication;
import org.example.client.cache.ServiceCache;
import org.example.client.serviceCenter.balance.ConsistencyHashBalance;
import org.example.client.serviceCenter.balance.LoadBalance;
import org.example.client.serviceCenter.balance.LruLoadBalance;
import org.example.client.serviceCenter.balance.RandomLoadBalance;
import org.example.client.serviceCenter.balance.RoundLoadBalance;
import org.example.client.serviceCenter.zkWatcher.ServiceChangeListener;
import org.example.client.serviceCenter.zkWatcher.ZkWatcher;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZkServiceCenter implements ServiceCenter {
    private static final String ROOT_PATH = "MyRpc";
    private static final String RETRY_PATH = "CanRetry";
    private static final long PROBE_INTERVAL_SECONDS = 10L;
    private static final int PROBE_TIMEOUT_MILLIS = 800;

    private final CuratorFramework client;
    private final ServiceCache serviceCache;
    private final ConcurrentMap<String, LoadBalance> loadBalanceMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> unavailableNodeMap = new ConcurrentHashMap<>();
    private final Set<String> retryServiceCache = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService probeExecutor;
    private final String loadBalanceType;

    public ZkServiceCenter() {
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000)
                .retryPolicy(policy)
                .namespace(ROOT_PATH)
                .build();
        this.client.start();
        this.serviceCache = new ServiceCache();
        this.loadBalanceType = KRpcApplication.getRpcConfig().getLoadBalance();
        this.probeExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "krpc-node-prober");
                thread.setDaemon(true);
                return thread;
            }
        });

        initRetryCache();
        registerWatcher();
        startProbeTask();
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        return serviceDiscovery(serviceName, null);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName, String requestKey) {
        try {
            List<String> addresses = serviceCache.getServiceFromCache(serviceName);
            if (addresses.isEmpty()) {
                addresses = loadAddressesFromRegistry(serviceName);
            }
            if (addresses.isEmpty()) {
                log.warn("No provider found for service={}", serviceName);
                return null;
            }

            List<String> availableAddresses = filterUnavailable(serviceName, addresses);
            if (availableAddresses.isEmpty()) {
                log.warn("All nodes are temporarily unavailable for service={}", serviceName);
                return null;
            }

            LoadBalance loadBalance = loadBalanceMap.computeIfAbsent(serviceName, key -> createLoadBalance());
            String picked = loadBalance.balance(availableAddresses, requestKey);
            return parseAddress(picked);
        } catch (Exception e) {
            log.error("Service discovery failed, service={}", serviceName, e);
            return null;
        }
    }

    @Override
    public boolean checkRetry(String serviceName) {
        return retryServiceCache.contains(serviceName);
    }

    @Override
    public void markNodeAsDown(String serviceName, InetSocketAddress address) {
        if (serviceName == null || serviceName.isEmpty() || address == null) {
            return;
        }
        String addressStr = toAddress(address);
        unavailableNodeMap.computeIfAbsent(serviceName, key -> ConcurrentHashMap.newKeySet()).add(addressStr);
        LoadBalance loadBalance = loadBalanceMap.get(serviceName);
        if (loadBalance != null) {
            loadBalance.delNode(addressStr);
        }
        log.warn("Mark node down, service={}, address={}", serviceName, addressStr);
    }

    @Override
    public void markNodeAsUp(String serviceName, InetSocketAddress address) {
        if (serviceName == null || serviceName.isEmpty() || address == null) {
            return;
        }
        String addressStr = toAddress(address);
        List<String> cached = serviceCache.getServiceFromCache(serviceName);
        if (!cached.contains(addressStr)) {
            return;
        }
        Set<String> unavailable = unavailableNodeMap.get(serviceName);
        if (unavailable != null) {
            unavailable.remove(addressStr);
        }
        LoadBalance loadBalance = loadBalanceMap.computeIfAbsent(serviceName, key -> createLoadBalance());
        loadBalance.addNode(addressStr);
        log.info("Mark node up, service={}, address={}", serviceName, addressStr);
    }

    private void initRetryCache() {
        try {
            if (client.checkExists().forPath("/" + RETRY_PATH) == null) {
                return;
            }
            List<String> services = client.getChildren().forPath("/" + RETRY_PATH);
            retryServiceCache.addAll(services);
        } catch (Exception e) {
            log.warn("Load retry whitelist from registry failed", e);
        }
    }

    private void registerWatcher() {
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onAdd(String serviceName, String address) {
                serviceCache.addServiceToCache(serviceName, address);
                loadBalanceMap.computeIfAbsent(serviceName, key -> createLoadBalance()).addNode(address);
                Set<String> unavailable = unavailableNodeMap.get(serviceName);
                if (unavailable != null) {
                    unavailable.remove(address);
                }
                log.info("Watcher add service node, service={}, address={}", serviceName, address);
            }

            @Override
            public void onRemove(String serviceName, String address) {
                serviceCache.delete(serviceName, address);
                LoadBalance loadBalance = loadBalanceMap.get(serviceName);
                if (loadBalance != null) {
                    loadBalance.delNode(address);
                }
                Set<String> unavailable = unavailableNodeMap.get(serviceName);
                if (unavailable != null) {
                    unavailable.remove(address);
                }
                log.info("Watcher remove service node, service={}, address={}", serviceName, address);
            }

            @Override
            public void onRetryAdd(String serviceName) {
                retryServiceCache.add(serviceName);
                log.info("Watcher add retry whitelist service={}", serviceName);
            }

            @Override
            public void onRetryRemove(String serviceName) {
                retryServiceCache.remove(serviceName);
                log.info("Watcher remove retry whitelist service={}", serviceName);
            }
        };

        ZkWatcher watcher = new ZkWatcher(client, listener);
        watcher.watchToUpdate("/");
    }

    private void startProbeTask() {
        probeExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    probeUnavailableNodes();
                } catch (Throwable throwable) {
                    log.error("Probe unavailable nodes failed", throwable);
                }
            }
        }, PROBE_INTERVAL_SECONDS, PROBE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void probeUnavailableNodes() {
        for (String serviceName : unavailableNodeMap.keySet()) {
            Set<String> unavailable = unavailableNodeMap.get(serviceName);
            if (unavailable == null || unavailable.isEmpty()) {
                continue;
            }
            List<String> snapshot = new ArrayList<>(unavailable);
            for (String address : snapshot) {
                InetSocketAddress socketAddress = parseAddress(address);
                if (socketAddress == null) {
                    unavailable.remove(address);
                    continue;
                }
                if (probeAddress(socketAddress)) {
                    markNodeAsUp(serviceName, socketAddress);
                    log.info("Probe recovered node, service={}, address={}", serviceName, address);
                }
            }
        }
    }

    private boolean probeAddress(InetSocketAddress address) {
        try (Socket socket = new Socket()) {
            socket.connect(address, PROBE_TIMEOUT_MILLIS);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<String> loadAddressesFromRegistry(String serviceName) throws Exception {
        if (client.checkExists().forPath("/" + serviceName) == null) {
            return new ArrayList<>();
        }
        List<String> addresses = client.getChildren().forPath("/" + serviceName);
        serviceCache.setServiceToCache(serviceName, addresses);
        LoadBalance loadBalance = loadBalanceMap.computeIfAbsent(serviceName, key -> createLoadBalance());
        for (String address : addresses) {
            loadBalance.addNode(address);
        }
        return addresses;
    }

    private List<String> filterUnavailable(String serviceName, List<String> addresses) {
        Set<String> unavailable = unavailableNodeMap.get(serviceName);
        if (unavailable == null || unavailable.isEmpty()) {
            return addresses;
        }
        List<String> available = new ArrayList<>(addresses.size());
        for (String address : addresses) {
            if (!unavailable.contains(address)) {
                available.add(address);
            }
        }
        return available;
    }

    private LoadBalance createLoadBalance() {
        String normalized = loadBalanceType == null ? "" : loadBalanceType.trim().toLowerCase();
        switch (normalized) {
            case "round":
            case "roundrobin":
                return new RoundLoadBalance();
            case "random":
                return new RandomLoadBalance();
            case "lru":
                return new LruLoadBalance();
            case "consistencyhash":
            case "consistenthash":
            case "hash":
            default:
                return new ConsistencyHashBalance();
        }
    }

    private String toAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostString() + ":" + serverAddress.getPort();
    }

    private InetSocketAddress parseAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        String[] result = address.split(":");
        if (result.length != 2) {
            return null;
        }
        try {
            return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
        } catch (NumberFormatException e) {
            log.warn("Invalid address format: {}", address, e);
            return null;
        }
    }
}
