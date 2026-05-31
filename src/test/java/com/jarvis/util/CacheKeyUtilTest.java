package com.jarvis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CacheKeyUtil 테스트")
class CacheKeyUtilTest {

    @Test
    @DisplayName("음성 응답 캐시 키 생성")
    void testGenerateVoiceResponseKey() {
        // Given
        String sessionId = "test-session-123";
        String fileHash = "abc123def456";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseKey(sessionId, fileHash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponse"));
        assertTrue(cacheKey.contains(sessionId));
        assertTrue(cacheKey.contains(fileHash));
    }

    @Test
    @DisplayName("음성 스트리밍 응답 캐시 키 생성")
    void testGenerateVoiceResponseStreamKey() {
        // Given
        String sessionId = "stream-session";
        String hash = "stream-hash";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseStreamKey(sessionId, hash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponseStream"));
        assertTrue(cacheKey.contains(sessionId));
    }

    @Test
    @DisplayName("대화 이력 캐시 키 생성")
    void testGenerateConversationHistoryKey() {
        // Given
        String sessionId = "history-session";

        // When
        String cacheKey = CacheKeyUtil.generateConversationHistoryKey(sessionId, 0, 10);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("conversationHistory"));
        assertTrue(cacheKey.contains(sessionId));
        assertTrue(cacheKey.contains("page:0"));
    }

    @Test
    @DisplayName("최근 대화 이력 캐시 키 생성")
    void testGenerateConversationHistoryRecentKey() {
        // Given
        String sessionId = "recent-session";

        // When
        String cacheKey = CacheKeyUtil.generateConversationHistoryRecentKey(sessionId);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("conversationHistoryRecent"));
        assertTrue(cacheKey.contains(sessionId));
    }

    @Test
    @DisplayName("텍스트 해싱")
    void testHashText() {
        // Given
        String text = "테스트 텍스트";

        // When
        String hash = CacheKeyUtil.hashText(text);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256은 64자 16진수
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("같은 입력은 같은 해시 생성")
    void testHashConsistency() {
        // Given
        byte[] data = "테스트 데이터".getBytes();

        // When
        String hash1 = CacheKeyUtil.hashBytes(data);
        String hash2 = CacheKeyUtil.hashBytes(data);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("다른 입력은 다른 해시 생성")
    void testHashDifference() {
        // Given
        byte[] data1 = "데이터 1".getBytes();
        byte[] data2 = "데이터 2".getBytes();

        // When
        String hash1 = CacheKeyUtil.hashBytes(data1);
        String hash2 = CacheKeyUtil.hashBytes(data2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("해시는 16진수 형식")
    void testHashIsHexadecimal() {
        // Given
        byte[] data = "테스트".getBytes();

        // When
        String hash = CacheKeyUtil.hashBytes(data);

        // Then
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("빈 바이트 배열 해싱")
    void testHashEmptyBytes() {
        // Given
        byte[] emptyData = new byte[0];

        // When
        String hash = CacheKeyUtil.hashBytes(emptyData);

        // Then
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("큰 데이터 해싱")
    void testHashLargeData() {
        // Given
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // When
        String hash = CacheKeyUtil.hashBytes(largeData);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256은 64자 16진수
    }

    @Test
    @DisplayName("캐시 키 NULL 검증")
    void testCacheKeyNullCheck() {
        // When & Then - NULL 파라미터 처리
        assertThrows(NullPointerException.class, () -> {
            CacheKeyUtil.generateVoiceResponseKey(null, "hash");
        });
    }
}
