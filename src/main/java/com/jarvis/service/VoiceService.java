package com.jarvis.service;

import com.jarvis.config.VoiceConfig;
import com.jarvis.dto.VoiceData;
import com.jarvis.exception.InvalidFileException;
import com.jarvis.exception.VoiceProcessingException;
import com.jarvis.tool.JarvisTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ChatClient chatClient;
    private final JarvisTools jarvisTools;

    public VoiceData processVoice(MultipartFile file) {
        validateFile(file);

        String id = UUID.randomUUID().toString();
        long fileSize = file.getSize();
        log.info("음성 처리 시작 - ID: {}, 크기: {}bytes", id, fileSize);

        // 1. STT 변환 (현재는 임시)
        String transcript = extractTranscriptFromFile(file);

        // 2. ChatClient로 처리 (Tool Calling 포함)
        String response;
        try {
            response = chatClient.prompt()
                .system("당신은 음성 명령 AI 어시스턴트입니다. 한국어로 친절하게 응답하세요.")
                .user(transcript)
                .tools(jarvisTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("ChatClient 처리 중 오류 발생 - ID: {}", id, e);
            throw new VoiceProcessingException("음성 처리 중 오류가 발생했습니다.", e);
        }

        log.info("음성 처리 완료 - ID: {}", id);

        return VoiceData.builder()
            .id(id)
            .transcript(transcript)
            .intent("사용자 요청 처리")
            .result(response)
            .build();
    }

    private String extractTranscriptFromFile(MultipartFile file) {
        // 현재는 임시: 파일명 제거
        // 나중에 Whisper API로 대체
        return "음성 입력 감지됨";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일을 선택해주세요.");
        }

        validateFileSize(file);
        validateFileExtension(file);
        validateContentType(file);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > VoiceConfig.MAX_FILE_SIZE) {
            throw new InvalidFileException(
                String.format("파일이 너무 큽니다. 최대 %dMB까지 업로드 가능합니다.",
                    VoiceConfig.MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("유효한 파일명이 필요합니다.");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!VoiceConfig.ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                String.format("지원하지 않는 파일 형식입니다. 허용 형식: %s",
                    String.join(", ", VoiceConfig.ALLOWED_EXTENSIONS))
            );
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new InvalidFileException("오디오 파일만 업로드 가능합니다.");
        }
    }
}