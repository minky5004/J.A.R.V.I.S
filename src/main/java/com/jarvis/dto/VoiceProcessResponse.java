package com.jarvis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoiceProcessResponse {
    private boolean success;        // 처리 성공 여부
    private String message;         // 응답 메시지
    private VoiceData data;         // 실제 데이터
}