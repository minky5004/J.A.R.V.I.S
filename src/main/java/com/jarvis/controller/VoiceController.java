package com.jarvis.controller;

import com.jarvis.dto.VoiceData;
import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}