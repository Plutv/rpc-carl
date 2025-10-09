package org.example.Client.serviceCenter;

import java.net.InetSocketAddress;

public interface ServiceCenter {
    InetSocketAddress serviceDiscovery(String serviceName);
}
