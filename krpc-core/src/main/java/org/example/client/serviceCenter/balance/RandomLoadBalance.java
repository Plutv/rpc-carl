package org.example.client.serviceCenter.balance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance implements org.example.client.serviceCenter.balance.LoadBalance {

    @Override
    public String balance(List<String> addressList) {
        Random random = new Random();
        int choose = random.nextInt(addressList.size());
        System.out.println("load balance server: " + choose);
        return addressList.get(choose);
    }

    @Override
    public void addNode(String node) {

    }

    @Override
    public void delNode(String node) {

    }
}
