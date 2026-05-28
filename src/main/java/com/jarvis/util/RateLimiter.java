package com.jarvis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimiter {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int TOKENS_PER_MINUTE = 10;
    private static final int MAX_TOKENS = 10;
    private static final long IDLE_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30분

    public RateLimiter() {
        scheduler.scheduleAtFixedRate(
            this::cleanupIdleBuckets,
            IDLE_TIMEOUT_MILLIS,
            IDLE_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        );
    }

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

    private void cleanupIdleBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry ->
            now - entry.getValue().getLastAccessTime() > IDLE_TIMEOUT_MILLIS
        );
        log.debug("유휴 버킷 정리 완료. 남은 버킷 수: {}", buckets.size());
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                log.warn("레이트 리미터 스케줄러 강제 종료");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("레이트 리미터 스케줄러 종료 중 인터럽트 발생", e);
        }
    }

    public static class TokenBucket {
        private double tokens;
        private final int maxTokens;
        private final int tokensPerMinute;
        private long lastRefillTime;
        private long lastAccessTime;

        public TokenBucket(int maxTokens, int tokensPerMinute) {
            this.tokens = maxTokens;
            this.maxTokens = maxTokens;
            this.tokensPerMinute = tokensPerMinute;
            this.lastRefillTime = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }

        public synchronized boolean consume() {
            lastAccessTime = System.currentTimeMillis();
            refill();

            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        public synchronized double getTokens() {
            lastAccessTime = System.currentTimeMillis();
            refill();
            return tokens;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            double tokensToAdd = (timePassed / 60000.0) * tokensPerMinute;

            tokens = Math.min(tokens + tokensToAdd, maxTokens);
            lastRefillTime = now;
        }
    }
}
