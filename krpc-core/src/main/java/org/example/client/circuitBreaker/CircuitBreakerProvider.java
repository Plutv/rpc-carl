package org.example.client.circuitBreaker;

import java.util.HashMap;
import java.util.Map;

public class CircuitBreakerProvider {
    private Map<String, org.example.client.circuitBreaker.CircuitBreaker> circuitBreakerMap = new HashMap<>();

    public org.example.client.circuitBreaker.CircuitBreaker getCircuitBreaker(String serviceName) {
        org.example.client.circuitBreaker.CircuitBreaker circuitBreaker;
        if (circuitBreakerMap.containsKey(serviceName)) {
            circuitBreaker = circuitBreakerMap.get(serviceName);
        } else {
            System.out.println("create new circuit breaker");
            circuitBreaker = new org.example.client.circuitBreaker.CircuitBreaker(1, 0.5, 10000);
            circuitBreakerMap.put(serviceName, circuitBreaker);
        }
        return circuitBreaker;
    }
}
