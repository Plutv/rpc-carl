package org.example.client.serviceCenter.balance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundLoadBalance implements org.example.client.serviceCenter.balance.LoadBalance {

   private AtomicInteger choose = new AtomicInteger(0);

    private List<String> addressList = new CopyOnWriteArrayList<>();

    @Override
    public String balance(List<String> addressList) {
        if (addressList == null || addressList.isEmpty()) {
            throw new IllegalArgumentException("addressList为空或者null");
        }
        int currentChoose = choose.getAndUpdate(i ->  (i + 1) % addressList.size());
        return addressList.get(currentChoose);
    }

    @Override
    public void addNode(String node) {

    }

    @Override
    public void delNode(String node) {

    }
}
