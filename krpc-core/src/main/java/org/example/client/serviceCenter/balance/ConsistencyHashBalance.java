package org.example.client.serviceCenter.balance;

import java.util.*;

public class ConsistencyHashBalance implements org.example.client.serviceCenter.balance.LoadBalance {
    public static final int VITUAL_NUM = 5;

    private SortedMap<Integer, String> shards = new TreeMap<Integer, String>();

    private List<String> realNodes = new LinkedList<String>();

    private String[] servers = null;

    private void init(List<String> serviceList) {
        for(String server: serviceList) {
            realNodes.add(server);
            System.out.println(server + " added!");
            for (int i = 0; i < VITUAL_NUM; i++) {
                String virtualNode = server + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.put(hash, virtualNode);
                System.out.println("virtual node: [" + virtualNode + "] hash: " + hash
                        + " added !");
            }
        }
    }

    public String getServer(String node, List<String> serviceList) {
        // 每次请求时，检查当前的节点列表与内部维护的 realNodes 是否一致
        // 如果节点数量发生变化，或者具体节点内容发生变化，则重新构建哈希环
        if (serviceList.size() != realNodes.size() || !new HashSet<>(serviceList).containsAll(realNodes)) {
            shards.clear();
            realNodes.clear();
            init(serviceList);
        }

        int hash = getHash(node);

        Integer key = null;
        SortedMap<Integer, String> subMap = shards.tailMap(hash);
        if (subMap.isEmpty()) {
            key = shards.firstKey();
        } else {
            key = subMap.firstKey();
        }
        String virtualNode = shards.get(key);
        return virtualNode.substring(0, virtualNode.indexOf("&&"));
    }

    @Override
    public String balance(List<String> addressList) {
        if (addressList == null || addressList.isEmpty()) {
            throw new IllegalArgumentException("Address List is null or empty!");
        }
        String random = UUID.randomUUID().toString();
        return getServer(random, addressList);
    }

    @Override
    public void addNode(String node) {
        if (!realNodes.contains(node)) {
            realNodes.add(node);
            System.out.println("real node [" + node + "] added !");
            for (int i = 0; i < VITUAL_NUM; i++) {
                String virtualNode = node + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.put(hash, virtualNode);
                System.out.println("virtual node [" + hash + "] added !");
            }
        }
    }

    @Override
    public void delNode(String node) {
        if (realNodes.contains(node)) {
            realNodes.remove(node);
            System.out.println("real node [" + node + "] removed !");
            for (int i = 0; i < VITUAL_NUM; i++) {
                String virtualNode = node + "&&VN" + i;
                int hash = getHash(virtualNode);
                shards.remove(hash, virtualNode);
                System.out.println("virtual node [" + hash + "] removed !");
            }
        }
    }

    private static int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
        }

        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        if (hash < 0) {
            hash = Math.abs(hash);
        }

        return hash;
    }
}
