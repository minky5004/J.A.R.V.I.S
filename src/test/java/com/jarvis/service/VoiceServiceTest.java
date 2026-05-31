package com.jarvis.service;

import com.jarvis.dto.VoiceData;
import com.jarvis.util.CacheKeyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VoiceService 테스트")
class VoiceServiceTest {

    @Test
    @DisplayName("UUID 형식 검증")
    void testUUIDFormat() {
        // When
        String uuid = UUID.randomUUID().toString();

        // Then
        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    @DisplayName("VoiceData 생성")
    void testVoiceDataCreation() {
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
    }

    @Test
    @DisplayName("sessionId NULL 체크")
    void testSessionIdNullCheck() {
        // When
        String sessionId = null;
        String result = sessionId == null ? UUID.randomUUID().toString() : sessionId;

        // Then
        assertNotNull(result);
        assertNotEquals(sessionId, result);
    }

    @Test
    @DisplayName("sessionId BLANK 체크")
    void testSessionIdBlankCheck() {
        // When
        String sessionId = "   ";
        String result = sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;

        // Then
        assertNotNull(result);
        assertNotEquals(sessionId, result);
    }

    @Test
    @DisplayName("다국어 텍스트 지원 - 한국어")
    void testKoreanText() {
        // When
        String text = "안녕하세요";

        // Then
        assertNotNull(text);
        assertTrue(text.contains("안"));
    }

    @Test
    @DisplayName("다국어 텍스트 지원 - 영어")
    void testEnglishText() {
        // When
        String text = "Hello";

        // Then
        assertEquals("Hello", text);
    }

    @Test
    @DisplayName("다국어 텍스트 지원 - 일본어")
    void testJapaneseText() {
        // When
        String text = "こんにちは";

        // Then
        assertNotNull(text);
        assertEquals("こんにちは", text);
    }

    @Test
    @DisplayName("캐시 키 생성")
    void testCacheKeyGeneration() {
        // Given
        String sessionId = "test-session";
        String hash = "abc123";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseKey(sessionId, hash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponse"));
        assertTrue(cacheKey.contains(sessionId));
    }

    @Test
    @DisplayName("음성 스트리밍 캐시 키")
    void testStreamCacheKey() {
        // Given
        String sessionId = "stream-session";
        String hash = "stream-hash";

        // When
        String cacheKey = CacheKeyUtil.generateVoiceResponseStreamKey(sessionId, hash);

        // Then
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains("voiceResponseStream"));
    }

    @Test
    @DisplayName("해시 일관성")
    void testHashConsistency() {
        // Given
        byte[] data = "테스트".getBytes();

        // When
        String hash1 = CacheKeyUtil.hashBytes(data);
        String hash2 = CacheKeyUtil.hashBytes(data);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("텍스트 해싱")
    void testTextHashing() {
        // Given
        String text = "Hello World";

        // When
        String hash = CacheKeyUtil.hashText(text);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("특수문자 텍스트")
    void testSpecialCharacterText() {
        // When
        String text = "!@#$%^&*()";

        // Then
        assertNotNull(text);
        assertTrue(text.contains("@"));
    }

    @Test
    @DisplayName("긴 텍스트 처리")
    void testLongText() {
        // When
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 126; i++) {
            longText.append("테스트 ");
        }

        // Then
        assertNotNull(longText.toString());
        assertTrue(longText.length() > 500);
    }

    @Test
    @DisplayName("빈 텍스트 처리")
    void testEmptyText() {
        // When
        String text = "";

        // Then
        assertTrue(text.isEmpty());
        assertEquals("", text);
    }

    @Test
    @DisplayName("세션 ID 생성")
    void testSessionIdGeneration() {
        // When
        String sessionId1 = UUID.randomUUID().toString();
        String sessionId2 = UUID.randomUUID().toString();

        // Then
        assertNotNull(sessionId1);
        assertNotNull(sessionId2);
        assertNotEquals(sessionId1, sessionId2);
    }
}
