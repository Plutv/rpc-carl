package org.example.client.circuitBreaker;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CircuitBreakerProvider {
    private Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    public synchronized CircuitBreaker getCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker;
        return circuitBreakerMap.computeIfAbsent(serviceName, key -> {
            log.info("服务{}不存在熔断器，新建一个熔断器实例", serviceName);
            return new CircuitBreaker(1, 0.5, 1000);
        });
    }
}
