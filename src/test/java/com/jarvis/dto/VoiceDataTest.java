package com.jarvis.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VoiceData DTO 테스트")
class VoiceDataTest {

    @Test
    @DisplayName("VoiceData 빌더 생성")
    void testVoiceDataBuilder() {
        // Given
        String id = UUID.randomUUID().toString();
        String sessionId = "test-session-123";
        String transcript = "안녕하세요";
        String language = "ko";
        String intent = "greeting";
        String resultText = "안녕하세요. 무엇을 도와드릴까요?";
        String result = "base64encodedaudio";

        // When
        VoiceData voiceData = VoiceData.builder()
                .id(id)
                .sessionId(sessionId)
                .transcript(transcript)
                .language(language)
                .intent(intent)
                .resultText(resultText)
                .result(result)
                .build();

        // Then
        assertNotNull(voiceData);
        assertEquals(id, voiceData.getId());
        assertEquals(sessionId, voiceData.getSessionId());
        assertEquals(transcript, voiceData.getTranscript());
        assertEquals(language, voiceData.getLanguage());
        assertEquals(intent, voiceData.getIntent());
        assertEquals(resultText, voiceData.getResultText());
        assertEquals(result, voiceData.getResult());
    }

    @Test
    @DisplayName("VoiceData 부분 빌더 생성")
    void testVoiceDataPartialBuilder() {
        // When
        VoiceData voiceData = VoiceData.builder()
                .id("test-id")
                .sessionId("test-session")
                .transcript("테스트")
                .build();

        // Then
        assertEquals("test-id", voiceData.getId());
        assertEquals("test-session", voiceData.getSessionId());
        assertEquals("테스트", voiceData.getTranscript());
        assertNull(voiceData.getLanguage());
        assertNull(voiceData.getIntent());
    }

    @Test
    @DisplayName("VoiceData toBuilder 사용")
    void testVoiceDataToBuilder() {
        // Given
        VoiceData original = VoiceData.builder()
                .id("id-1")
                .sessionId("session-1")
                .transcript("원본")
                .language("ko")
                .build();

        // When - ID만 변경
        VoiceData modified = original.toBuilder()
                .id("id-2")
                .build();

        // Then
        assertEquals("id-2", modified.getId());
        assertEquals("session-1", modified.getSessionId());
        assertEquals("원본", modified.getTranscript());
        assertEquals("ko", modified.getLanguage());
    }

    @Test
    @DisplayName("VoiceData 한국어 데이터")
    void testVoiceDataKorean() {
        // When
        VoiceData voiceData = VoiceData.builder()
                .id("123")
                .sessionId("session")
                .transcript("내일 날씨가 어때?")
                .language("ko")
                .intent("weatherInfo")
                .resultText("내일 서울의 날씨는 맑겠습니다.")
                .build();

        // Then
        assertEquals("ko", voiceData.getLanguage());
        assertEquals("weatherInfo", voiceData.getIntent());
        assertTrue(voiceData.getTranscript().contains("날씨"));
    }

    @Test
    @DisplayName("VoiceData 영어 데이터")
    void testVoiceDataEnglish() {
        // When
        VoiceData voiceData = VoiceData.builder()
                .id("456")
                .sessionId("session")
                .transcript("What is the weather tomorrow?")
                .language("en")
                .intent("weatherInfo")
                .resultText("Tomorrow in Seoul, it will be sunny.")
                .build();

        // Then
        assertEquals("en", voiceData.getLanguage());
        assertNotNull(voiceData.getTranscript());
    }
}
