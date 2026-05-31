package com.jarvis.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiter 테스트")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    @DisplayName("첫 요청은 항상 허용")
    void testFirstRequestAllowed() {
        // Given
        String identifier = "test-session";

        // When
        boolean allowed = rateLimiter.allowRequest(identifier);

        // Then
        assertTrue(allowed);
    }

    @Test
    @DisplayName("제한 내 요청은 모두 허용")
    void testRequestsWithinLimit() {
        // Given
        String identifier = "test-session";
        int requestCount = 10; // 분당 10회

        // When & Then
        for (int i = 0; i < requestCount; i++) {
            assertTrue(rateLimiter.allowRequest(identifier));
        }
    }

    @Test
    @DisplayName("제한 초과 요청은 거부")
    void testRequestsExceedingLimit() {
        // Given
        String identifier = "test-session";

        // When - 10회 이상 요청
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowRequest(identifier);
        }
        boolean eleventh = rateLimiter.allowRequest(identifier);

        // Then
        assertFalse(eleventh);
    }

    @Test
    @DisplayName("다른 identifier는 별도 제한")
    void testDifferentIdentifiersHaveSeparateLimits() {
        // Given
        String identifier1 = "session-1";
        String identifier2 = "session-2";

        // When - identifier1에서 10회
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowRequest(identifier1);
        }

        // Then - identifier2는 여전히 허용
        assertTrue(rateLimiter.allowRequest(identifier2));
    }

    @Test
    @DisplayName("특정 identifier로 여러 요청")
    void testMultipleRequestsSpecificIdentifier() {
        // Given
        String identifier = "heavy-user";

        // When - 5회 요청
        int successCount = 0;
        for (int i = 0; i < 5; i++) {
            if (rateLimiter.allowRequest(identifier)) {
                successCount++;
            }
        }

        // Then
        assertEquals(5, successCount);
    }

    @Test
    @DisplayName("분당 10회 제한 검증")
    void testPerMinuteLimitIs10() {
        // Given
        String identifier = "test-limit";

        // When - 10회 요청
        boolean[] results = new boolean[11];
        for (int i = 0; i < 11; i++) {
            results[i] = rateLimiter.allowRequest(identifier);
        }

        // Then - 처음 10개는 true, 11번째는 false
        for (int i = 0; i < 10; i++) {
            assertTrue(results[i], "요청 " + i + "는 허용되어야 함");
        }
        assertFalse(results[10], "11번째 요청은 거부되어야 함");
    }

    @Test
    @DisplayName("Token Bucket 알고리즘 동작")
    void testTokenBucketAlgorithm() {
        // Given
        String identifier = "bucket-test";

        // When - 초기 10개 토큰 사용
        boolean[] initialRequests = new boolean[10];
        for (int i = 0; i < 10; i++) {
            initialRequests[i] = rateLimiter.allowRequest(identifier);
        }

        // Then - 모두 성공
        for (boolean result : initialRequests) {
            assertTrue(result);
        }

        // 11번째는 실패
        assertFalse(rateLimiter.allowRequest(identifier));
    }

    @Test
    @DisplayName("남은 토큰 조회")
    void testGetRemainingTokens() {
        // Given
        String identifier = "token-test";

        // When
        int initialTokens = rateLimiter.getRemainingTokens(identifier);
        rateLimiter.allowRequest(identifier);
        int tokensAfterRequest = rateLimiter.getRemainingTokens(identifier);

        // Then
        assertEquals(10, initialTokens);
        assertEquals(9, tokensAfterRequest);
    }

    @Test
    @DisplayName("여러 식별자 동시 처리")
    void testMultipleIdentifiersConcurrently() {
        // Given
        String id1 = "user-1";
        String id2 = "user-2";
        String id3 = "user-3";

        // When
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowRequest(id1);
            rateLimiter.allowRequest(id2);
            rateLimiter.allowRequest(id3);
        }

        // Then - 모두 5회씩 성공 가능
        assertTrue(rateLimiter.allowRequest(id1));
        assertTrue(rateLimiter.allowRequest(id2));
        assertTrue(rateLimiter.allowRequest(id3));
    }
}
