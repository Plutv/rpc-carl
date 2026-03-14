package org.example.client.serviceCenter.zkWatcher;

/**
 * Zookeeper 服务节点变化回调。
 * 由 ZkServiceCenter 统一协调：更新本地缓存 + 更新对应服务的一致性哈希环。
 */
public interface ServiceChangeListener {

    void onAdd(String serviceName, String address);

    void onRemove(String serviceName, String address);

    default void onReplace(String serviceName, String oldAddress, String newAddress) {
        onRemove(serviceName, oldAddress);
        onAdd(serviceName, newAddress);
    }
}

