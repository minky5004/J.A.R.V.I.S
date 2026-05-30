package com.jarvis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@Schema(description = "음성 처리 결과 데이터")
public class VoiceData {
    @Schema(description = "처리 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "세션 ID", example = "user-12345")
    private String sessionId;

    @Schema(description = "STT 변환 결과 텍스트", example = "내일 날씨가 어때?")
    private String transcript;

    @Schema(description = "감지된 언어 코드", example = "ko", allowableValues = {"ko", "en", "ja"})
    private String language;

    @Schema(description = "AI가 파악한 의도", example = "weatherInfo")
    private String intent;

    @Schema(description = "AI 응답 원본 텍스트", example = "내일 서울의 날씨는 맑겠으며 기온은 25도입니다.")
    private String resultText;

    @Schema(description = "음성으로 변환된 base64 오디오", example = "//NXAAA/AAAAAQ==...")
    private String result;
}