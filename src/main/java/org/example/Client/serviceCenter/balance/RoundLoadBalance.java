package org.example.Client.serviceCenter.balance;

import java.util.List;

public class RoundLoadBalance implements LoadBalance{
    private int choose = -1;

    @Override
    public String balance(List<String> addressList) {
        choose++;
        choose = choose % addressList.size();
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
