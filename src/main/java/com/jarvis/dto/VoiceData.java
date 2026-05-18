package com.jarvis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoiceData {
    private String id;              // 처리 ID (UUID)
    private String transcript;      // STT 변환 결과 텍스트
    private String intent;          // AI가 파악한 의도 (날씨조회, 일정확인 등)
    private String result;          // 실행 결과
}