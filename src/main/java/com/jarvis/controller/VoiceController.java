package com.jarvis.controller;

import com.jarvis.dto.VoiceData;
import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService voiceService;

    @PostMapping("/process")
    public ResponseEntity<VoiceProcessResponse> processVoice(
        @RequestParam("file") MultipartFile file
    ) {
        log.info("음성 처리 요청 - 크기: {}bytes", file.getSize());

        VoiceData voiceData = voiceService.processVoice(file);

        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(true)
            .message("음성 파일이 성공적으로 처리되었습니다.")
            .data(voiceData)
            .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/test")
    public ResponseEntity<VoiceProcessResponse> processTest(
        @RequestBody Map<String, String> request
    ) {
        if (request == null || request.get("text") == null) {
            throw new IllegalArgumentException("text 필드가 필요합니다.");
        }
        String text = request.get("text");
        log.info("테스트 요청 - 텍스트 길이: {}글자", text.length());

        VoiceData voiceData = voiceService.processText(text);

        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(true)
            .message("테스트 요청이 성공적으로 처리되었습니다.")
            .data(voiceData)
            .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}