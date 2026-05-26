package com.jarvis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoiceProcessResponse<T> {
    private boolean success;        // 처리 성공 여부
    private String message;         // 응답 메시지
    private T data;                 // 실제 데이터

    public static <T> VoiceProcessResponse<T> success(String message, T data) {
        return VoiceProcessResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> VoiceProcessResponse<T> failure(String message) {
        return VoiceProcessResponse.<T>builder()
            .success(false)
            .message(message)
            .data(null)
            .build();
    }
}