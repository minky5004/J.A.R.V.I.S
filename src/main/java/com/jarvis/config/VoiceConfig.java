package com.jarvis.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

@Configuration
public class VoiceConfig {
    // 파일 업로드 제약사항
    public static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB (OpenAI Whisper 제한)
    public static final long MAX_REQUEST_SIZE = 30 * 1024 * 1024; // 30MB

    // 허용되는 파일 확장자
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "wav", "mp3", "m4a", "flac", "ogg"
    );

    // 허용되는 MIME 타입
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "audio/wav",
        "audio/mpeg",
        "audio/mp4",
        "audio/flac",
        "audio/ogg"
    );

    // 임시 파일 저장 디렉토리
    public static final String TEMP_DIR = "temp/voice";

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}