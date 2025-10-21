package org.example.server.ratelimit;

public class TokenBucketRateLimitImpl implements org.example.server.ratelimit.RateLimit {

    private static int RATE;
    private static int CAPACITY;
    private volatile int curCapicity;
    private volatile long timeStamp = System.currentTimeMillis();

    public TokenBucketRateLimitImpl(int rate, int capicity) {
        RATE = rate;
        CAPACITY = capicity;
        curCapicity = capicity;
    }

    @Override
    public boolean getToken() {
        if (curCapicity > 0) {
            curCapicity--;
            return true;
        }
        long current = System.currentTimeMillis();
        if (current - timeStamp >= RATE) {
            if ((current - timeStamp) / RATE >= 2) {
                curCapicity += ((current - timeStamp) / RATE) - 1;
            }
            if (curCapicity > CAPACITY) {
                curCapicity = CAPACITY;
            }
            timeStamp = current;
            return true;
        }
        return false;
    }
}
