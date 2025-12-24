package org.example.client.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServiceCache {
    private static Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public void addServiceToCache(String serviceName, String address) {
        if (cache.containsKey(serviceName)) {
            List<String> addressList = cache.get(serviceName);
            addressList.add(address);
            System.out.println("添加服务到本地缓存: " + serviceName + " " + address);
        } else {
            List<String> addressList = new ArrayList<>();
            addressList.add(address);
            cache.put(serviceName, addressList);
        }
    }

    public void replaceServiceAddress(String serviceName, String oldAddress, String newAddress) {
        if (cache.containsKey(serviceName)) {
            List<String> addressList = cache.get(serviceName);
            addressList.remove(oldAddress);
            addressList.add(newAddress);
        } else {
            System.out.println("replace failed!");
        }
    }

    public List<String> getServiceFromCache(String serviceName) {
        if (!cache.containsKey(serviceName)) {
            log.warn("服务未找到：", serviceName);
            return Collections.emptyList();
        }
        return cache.get(serviceName);
    }

    public void delete(String serviceName, String address) {
        List<String> addressList = cache.get(serviceName);
        if (addressList != null && addressList.contains(serviceName)) {
            addressList.remove(address);
            log.info("将name为{}和地址为{}的服务从本地缓存中删除", serviceName, address);
            if (addressList.isEmpty()) {
                cache.remove(serviceName);
                log.info("服务{}的地址列表为空，从缓存中清除", serviceName);
            }
        } else {
            log.warn("删除失败，地址不在服务列表中");
        }
    }
}
