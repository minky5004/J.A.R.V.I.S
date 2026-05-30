package com.jarvis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "음성 처리 응답")
public class VoiceProcessResponse<T> {
    @Schema(description = "처리 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "음성 파일이 성공적으로 처리되었습니다.")
    private String message;

    @Schema(description = "실제 데이터")
    private T data;

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