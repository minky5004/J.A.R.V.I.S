package com.jarvis.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.util.Set;

@Configuration
public class VoiceConfig {
    public static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
    @SuppressWarnings("unused")
    public static final long MAX_REQUEST_SIZE = 30 * 1024 * 1024; // 30MB
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("wav", "mp3", "m4a", "flac", "ogg", "webm");
    @SuppressWarnings("unused")
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("audio/wav", "audio/mpeg", "audio/mp4", "audio/flac", "audio/ogg");
    @SuppressWarnings("unused")
    public static final String TEMP_DIR = new File(System.getProperty("java.io.tmpdir"), "jarvis/voice").getAbsolutePath();

    @Value("${api.timeout.connect:10000}")
    private int connectTimeout;

    @Value("${api.timeout.read:30000}")
    private int readTimeout;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}