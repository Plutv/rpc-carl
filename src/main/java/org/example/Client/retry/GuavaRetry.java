package org.example.Client.retry;

import com.github.rholder.retry.*;
import org.example.Client.rpcClient.RpcClient;
import org.example.common.message.RpcRequest;
import org.example.common.message.RpcResponse;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GuavaRetry {
    private RpcClient client;

    public RpcResponse sendServiceWithRetry(RpcRequest request, RpcClient client) {
        this.client = client;
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfException()
                .retryIfResult(response -> Objects.equals(response.getCode(), 500))
                .withWaitStrategy(WaitStrategies.fixedWait(2, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        System.out.println("retry listener: " + attempt.getAttemptNumber());
                    }
                })
                .build();
        try {
            return retryer.call(() -> client.sendRequest(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RpcResponse.fail();
    }
}
