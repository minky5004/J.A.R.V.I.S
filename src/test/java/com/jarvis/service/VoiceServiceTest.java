package com.jarvis.service;

import com.jarvis.dto.VoiceData;
import com.jarvis.util.CacheKeyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoiceService 및 관련 유틸리티 통합 테스트.
 * CacheKeyUtil과 VoiceData를 통한 음성 처리 흐름 검증.
 */
@DisplayName("VoiceService 통합 테스트")
class VoiceServiceTest {

    @Test
    @DisplayName("VoiceData 빌더 검증")
    void testVoiceDataBuilder() {
        // When
        VoiceData voiceData = VoiceData.builder()
                .id("test-id")
                .sessionId("test-session")
                .transcript("테스트")
                .build();

        // Then
        assertNotNull(voiceData);
        assertEquals("test-id", voiceData.getId());
        assertEquals("test-session", voiceData.getSessionId());
        assertEquals("테스트", voiceData.getTranscript());
    }

    @Test
    @DisplayName("세션별 캐시 키 생성")
    void testSessionCacheKeyGeneration() {
        // Given
        String sessionId = "user-123";
        String hash = "abc123def456";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseKey(sessionId, hash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponse"));
        assertTrue(cacheKey.contains(sessionId));
        assertTrue(cacheKey.contains(hash));
    }

    @Test
    @DisplayName("스트리밍 응답 캐시 키")
    void testStreamingCacheKey() {
        // Given
        String sessionId = "stream-user-456";
        String hash = "stream-hash-789";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseStreamKey(sessionId, hash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponseStream"));
        assertTrue(cacheKey.contains(sessionId));
    }

    @Test
    @DisplayName("대화 이력 캐시 키 페이지네이션")
    void testConversationHistoryCacheKey() {
        // Given
        String sessionId = "chat-session-789";
        int page = 2;
        int size = 20;

        // When
        String cacheKey = CacheKeyUtil.generateConversationHistoryKey(sessionId, page, size);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("conversationHistory"));
        assertTrue(cacheKey.contains(sessionId));
        assertTrue(cacheKey.contains("page:" + page));
    }

    @Test
    @DisplayName("최근 대화 이력 캐시 키")
    void testRecentConversationCacheKey() {
        // Given
        String sessionId = "recent-session-101";

        // When
        String cacheKey = CacheKeyUtil.generateConversationHistoryRecentKey(sessionId);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("conversationHistoryRecent"));
        assertTrue(cacheKey.contains(sessionId));
    }

    @Test
    @DisplayName("다국어 텍스트 해싱 (한국어)")
    void testKoreanTextHashing() {
        // Given
        String text = "안녕하세요";

        // When
        String hash = CacheKeyUtil.hashText(text);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("다국어 텍스트 해싱 (일본어)")
    void testJapaneseTextHashing() {
        // Given
        String text = "こんにちは";

        // When
        String hash = CacheKeyUtil.hashText(text);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("해시 일관성 검증")
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
    @DisplayName("대용량 데이터 해싱")
    void testLargeDataHashing() {
        // Given
        byte[] largeData = new byte[512 * 1024]; // 512KB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // When
        String hash = CacheKeyUtil.hashBytes(largeData);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("빈 데이터 해싱")
    void testEmptyDataHashing() {
        // Given
        byte[] emptyData = new byte[0];

        // When
        String hash = CacheKeyUtil.hashBytes(emptyData);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("NULL 파라미터 검증")
    void testNullParameterValidation() {
        // Then
        assertThrows(NullPointerException.class, () -> {
            CacheKeyUtil.generateVoiceResponseKey(null, "hash");
        });
    }
}
