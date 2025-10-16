package org.example.Client.proxy;

import lombok.AllArgsConstructor;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;
import org.example.Client.rpcClient.impl.NettyRpcClient;
import org.example.Client.rpcClient.RpcClient;
import org.example.Client.rpcClient.impl.SimpleSocketRpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
public class ClientProxy implements InvocationHandler {

    private RpcClient rpcClient;

    public ClientProxy() throws InterruptedException{
        rpcClient = new NettyRpcClient();
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
        RpcResponse response = rpcClient.sendRequest(request);
        return response.getData();
    }

    public <T>T getProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
