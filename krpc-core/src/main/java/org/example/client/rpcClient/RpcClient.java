package org.example.client.rpcClient;

import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;

public interface RpcClient {
    RpcResponse sendRequest(RpcRequest rpcRequest);
}
