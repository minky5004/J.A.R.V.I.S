package com.jarvis.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VoiceProcessResponse DTO 테스트")
class VoiceProcessResponseTest {

    @Test
    @DisplayName("VoiceProcessResponse 성공 응답 생성")
    void testVoiceProcessResponseSuccess() {
        // Given
        VoiceData voiceData = VoiceData.builder()
                .id("test-id")
                .sessionId("test-session")
                .transcript("테스트")
                .build();

        // When
        VoiceProcessResponse<VoiceData> response = VoiceProcessResponse.success("처리 완료", voiceData);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("처리 완료", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("test-id", response.getData().getId());
    }

    @Test
    @DisplayName("VoiceProcessResponse 실패 응답 생성")
    void testVoiceProcessResponseFailure() {
        // When
        VoiceProcessResponse<String> response = VoiceProcessResponse.failure("오류 발생");

        // Then
        assertFalse(response.isSuccess());
        assertEquals("오류 발생", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("VoiceProcessResponse 빌더 생성")
    void testVoiceProcessResponseBuilder() {
        // When
        VoiceProcessResponse<String> response = VoiceProcessResponse.<String>builder()
                .success(true)
                .message("성공")
                .data("데이터")
                .build();

        // Then
        assertTrue(response.isSuccess());
        assertEquals("성공", response.getMessage());
        assertEquals("데이터", response.getData());
    }

    @Test
    @DisplayName("VoiceProcessResponse 제네릭 타입 테스트")
    void testVoiceProcessResponseGeneric() {
        // Given
        VoiceData voiceData = VoiceData.builder()
                .id("123")
                .sessionId("session")
                .transcript("테스트")
                .build();

        // When
        VoiceProcessResponse<VoiceData> response = VoiceProcessResponse.success("완료", voiceData);

        // Then
        assertNotNull(response.getData());
        assertTrue(response.getData() instanceof VoiceData);
        assertEquals("123", response.getData().getId());
    }

    @Test
    @DisplayName("VoiceProcessResponse 실패 메시지")
    void testVoiceProcessResponseFailureMessage() {
        // When
        VoiceProcessResponse<?> response = VoiceProcessResponse.failure("파일 처리 오류");

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("오류"));
        assertNull(response.getData());
    }

    @Test
    @DisplayName("VoiceProcessResponse String 응답")
    void testVoiceProcessResponseStringData() {
        // When
        VoiceProcessResponse<String> response = VoiceProcessResponse.success("정상 처리", "응답 데이터");

        // Then
        assertTrue(response.isSuccess());
        assertEquals("응답 데이터", response.getData());
    }
}
