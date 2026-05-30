package com.jarvis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Swagger/OpenAPI 3.0 설정
 * 자동으로 생성되는 API 문서를 통해 REST 엔드포인트를 시각화하고 테스트 가능하게 함
 *
 * 접근 경로:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 스키마 커스터마이징
     * 서버 정보, API 메타정보, 연락처, 라이선스 등을 정의
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .servers(Arrays.asList(
                new Server().url("http://localhost:8080").description("로컬 개발 환경"),
                new Server().url("https://api.jarvis.example.com").description("프로덕션 환경")
            ))
            .info(new Info()
                .title("J.A.R.V.I.S API")
                .description("자연어 기반 음성 명령 처리 AI 어시스턴트 백엔드 서비스\n\n" +
                    "음성 파일을 입력받아 STT(Speech-to-Text)로 변환하고, " +
                    "GPT-4o-mini를 이용하여 의도를 파악한 후 " +
                    "적절한 Tool을 호출하여 결과를 생성하고 " +
                    "TTS(Text-to-Speech)로 변환하여 반환합니다.\n\n" +
                    "**주요 기능**\n" +
                    "- Voice to Text: Whisper API를 통한 음성 인식\n" +
                    "- Natural Language Understanding: GPT-4o-mini를 이용한 의도 파악\n" +
                    "- Tool Calling: 8개의 내장 Tool을 자동으로 실행\n" +
                    "- Text to Speech: OpenAI TTS API를 이용한 음성 생성\n" +
                    "- Conversation History: PostgreSQL 기반 대화 이력 관리\n" +
                    "- Response Caching: Redis 기반 응답 캐싱\n" +
                    "- Rate Limiting: Token Bucket 알고리즘 기반 요청 제한\n" +
                    "- Server-Sent Events: 실시간 스트리밍 응답\n\n" +
                    "**Available Tools**\n" +
                    "1. weatherInfo() - 날씨 조회 (Open-Meteo API)\n" +
                    "2. searchNews() - 뉴스 검색 (DuckDuckGo)\n" +
                    "3. searchWeb() - 웹 검색 (DuckDuckGo)\n" +
                    "4. translate() - 텍스트 번역 (MyMemory)\n" +
                    "5. calculate() - 계산\n" +
                    "6. convertTime() - 시간 변환\n" +
                    "7. convertUnit() - 단위 변환\n" +
                    "8. speakText() - 텍스트 음성 변환")
                .version("1.0.0")
                .contact(new Contact()
                    .name("J.A.R.V.I.S Team")
                    .email("minky5004@gmail.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")));
    }
}