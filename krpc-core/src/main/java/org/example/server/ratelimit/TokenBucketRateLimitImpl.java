package org.example.server.ratelimit;

public class TokenBucketRateLimitImpl implements org.example.server.ratelimit.RateLimit {

    private int RATE;
    private int CAPACITY;
    private volatile int curCapicity;
    private volatile long lastTimeStamp; // 上次请求时间戳

    public TokenBucketRateLimitImpl(int rate, int capicity) {
        RATE = rate;
        CAPACITY = capicity;
        curCapicity = capicity;
        lastTimeStamp = System.currentTimeMillis();
    }

    @Override
    public boolean getToken() {
        synchronized (this) {
            if (curCapicity > 0) {
                curCapicity--;
                return true;
            }
            long current = System.currentTimeMillis();
            if (current - lastTimeStamp >= RATE) {
                if ((current - lastTimeStamp) / RATE >= 2) {
                    curCapicity += (int) (((current - lastTimeStamp) / RATE) - 1);
                }
                if (curCapicity > CAPACITY) {
                    curCapicity = CAPACITY;
                }
                lastTimeStamp = current;
                return true;
            }
            return false;
        }

    }
}
