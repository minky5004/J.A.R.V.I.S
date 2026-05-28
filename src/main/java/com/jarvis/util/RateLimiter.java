package com.jarvis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RateLimiter {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static final int TOKENS_PER_MINUTE = 10;
    private static final int MAX_TOKENS = 10;

    public boolean allowRequest(String identifier) {
        TokenBucket bucket = buckets.computeIfAbsent(identifier,
            k -> new TokenBucket(MAX_TOKENS, TOKENS_PER_MINUTE));

        return bucket.consume();
    }

    public int getRemainingTokens(String identifier) {
        TokenBucket bucket = buckets.get(identifier);
        if (bucket == null) {
            return MAX_TOKENS;
        }
        return (int) bucket.getTokens();
    }

    public static class TokenBucket {
        private double tokens;
        private final int tokensPerMinute;
        private long lastRefillTime;

        public TokenBucket(int maxTokens, int tokensPerMinute) {
            this.tokens = maxTokens;
            this.tokensPerMinute = tokensPerMinute;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean consume() {
            refill();

            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        public synchronized double getTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            double tokensToAdd = (timePassed / 60000.0) * tokensPerMinute;

            tokens = Math.min(tokens + tokensToAdd, MAX_TOKENS);
            lastRefillTime = now;
        }
    }
}
