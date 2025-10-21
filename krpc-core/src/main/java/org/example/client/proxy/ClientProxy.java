package org.example.client.proxy;

import lombok.AllArgsConstructor;
import org.example.client.circuitBreaker.CircuitBreaker;
import org.example.client.circuitBreaker.CircuitBreakerProvider;
import org.example.client.retry.GuavaRetry;
import org.example.client.serviceCenter.ServiceCenter;
import org.example.client.serviceCenter.ZkServiceCenter;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.client.rpcClient.impl.NettyRpcClient;
import org.example.client.rpcClient.RpcClient;
import org.example.client.rpcClient.impl.SimpleSocketRpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
public class ClientProxy implements InvocationHandler {

    private RpcClient rpcClient;

    private ServiceCenter serviceCenter;

    private CircuitBreakerProvider circuitBreakerProvider;

    public ClientProxy() throws InterruptedException{
        serviceCenter = new ZkServiceCenter();
        rpcClient = new NettyRpcClient(serviceCenter);
        circuitBreakerProvider = new CircuitBreakerProvider();
    }

    public ClientProxy(String host, int port, int choose) {
        switch (choose) {
            case 0:
                rpcClient = new NettyRpcClient(host, port);
                break;
            case 1:
                rpcClient = new SimpleSocketRpcClient(host, port);
        }
    }

    public ClientProxy(String host, int port) {
        rpcClient = new NettyRpcClient(host, port);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        RpcRequest request = RpcRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args)
                .paramsType(method.getParameterTypes()).build();

        CircuitBreaker circuitBreaker = circuitBreakerProvider.getCircuitBreaker(method.getName());

        if (!circuitBreaker.allowRequest()) {
            return null;
        }

        RpcResponse response;
        if (serviceCenter.checkRetry(request.getInterfaceName())) {
            response = new GuavaRetry().sendServiceWithRetry(request, rpcClient);
        } else {
            response = rpcClient.sendRequest(request);
        }
        return response.getData();
    }

    public <T>T getProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
