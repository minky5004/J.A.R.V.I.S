package com.jarvis.service;

import com.jarvis.config.VoiceConfig;
import com.jarvis.dto.VoiceData;
import com.jarvis.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class VoiceService {

    public VoiceData processVoice(MultipartFile file) {
        validateFile(file);

        String id = UUID.randomUUID().toString();
        log.info("음성 처리 시작 - ID: {}, 파일명: {}", id, file.getOriginalFilename());

        return VoiceData.builder()
            .id(id)
            .transcript(null)  // 나중에 Whisper STT 결과 저장
            .intent(null)       // 나중에 GPT 의도 파악 결과 저장
            .result(null)       // 나중에 Tool Calling 결과 저장
            .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일을 선택해주세요.");
        }

        validateFileSize(file);
        validateFileExtension(file);
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
}