package org.example.client.netty;

import lombok.extern.slf4j.Slf4j;
import org.example.common.message.RpcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public final class PendingRequests {
    private static final ConcurrentMap<String, CompletableFuture<RpcResponse>> FUTURE_MAP = new ConcurrentHashMap<>();

    private PendingRequests() {
    }

    public static void put(String requestId, CompletableFuture<RpcResponse> future) {
        FUTURE_MAP.put(requestId, future);
    }

    public static void complete(RpcResponse response) {
        if (response == null || response.getRequestId() == null) {
            return;
        }
        CompletableFuture<RpcResponse> future = FUTURE_MAP.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
            return;
        }
        log.warn("No pending request found for requestId={}", response.getRequestId());
    }

    public static void fail(String requestId, Throwable throwable) {
        CompletableFuture<RpcResponse> future = FUTURE_MAP.remove(requestId);
        if (future != null) {
            future.completeExceptionally(throwable);
        }
    }

    public static void remove(String requestId) {
        FUTURE_MAP.remove(requestId);
    }
}
