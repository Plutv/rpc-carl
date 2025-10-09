package org.example.Server.provider;

import org.example.Server.serviceRegister.ServiceRegister;
import org.example.Server.serviceRegister.impl.ZKServiceRegister;

import javax.imageio.spi.ServiceRegistry;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ServiceProvider {
    private Map<String, Object> interfaceProvider;

    private int port;
    private String host;

    private ServiceRegister serviceRegister;

    public ServiceProvider(String host, int port) {
        this.host = host;
        this.port = port;
        this.interfaceProvider = new HashMap<>();
        this.serviceRegister = new ZKServiceRegister();
    }

    public ServiceProvider() {
        this.interfaceProvider = new HashMap<>();
    }

    public void provideServiceInterface(Object service) {
        String serviceName = service.getClass().getName();
        Class<?>[] interfaceName = service.getClass().getInterfaces();

        for(Class<?> clazz:interfaceName) {
            interfaceProvider.put(clazz.getName(), service);
            serviceRegister.register(clazz.getName(), new InetSocketAddress(host, port));
        }
    }

    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }
}
