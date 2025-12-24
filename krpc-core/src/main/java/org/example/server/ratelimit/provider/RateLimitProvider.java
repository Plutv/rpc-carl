package org.example.server.ratelimit.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.server.ratelimit.RateLimit;
import org.example.server.ratelimit.TokenBucketRateLimitImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitProvider {
    private Map<String, RateLimit> rateLimitMap = new ConcurrentHashMap<>();

    private static final int DEFAULT_CAPICITY = 100;
    private static final int DEFAULT_RATE = 10;

    public RateLimit getRateLimit(String interfaceName) {
        if (!rateLimitMap.containsKey(interfaceName)) {
            RateLimit rateLimit = new TokenBucketRateLimitImpl(DEFAULT_CAPICITY, DEFAULT_RATE);
            log.info("为接口创建新的限流策略");
            rateLimitMap.put(interfaceName, rateLimit);
            return rateLimit;
        }
        return rateLimitMap.get(interfaceName);
    }
}