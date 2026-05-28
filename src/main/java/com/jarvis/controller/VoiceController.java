package com.jarvis.controller;

import com.jarvis.dto.VoiceData;
import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.service.VoiceService;
import com.jarvis.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService voiceService;
    private final RateLimiter rateLimiter;

    @PostMapping("/process")
    public ResponseEntity<VoiceProcessResponse<VoiceData>> processVoice(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "sessionId", required = false) String sessionId
    ) {
        String identifier = sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;

        if (!rateLimiter.allowRequest(identifier)) {
            log.warn("레이트 리미팅 초과 - identifier: {}", identifier);
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.info("음성 처리 요청 - 크기: {}bytes, sessionId: {}", file.getSize(), sessionId);

        VoiceData voiceData = voiceService.processVoice(file, sessionId);

        return ResponseEntity.status(HttpStatus.OK).body(
            VoiceProcessResponse.success("음성 파일이 성공적으로 처리되었습니다.", voiceData)
        );
    }

    @PostMapping("/test")
    public ResponseEntity<VoiceProcessResponse<VoiceData>> processTest(
        @RequestBody Map<String, String> request
    ) {
        if (request == null || request.get("text") == null) {
            throw new IllegalArgumentException("text 필드가 필요합니다.");
        }
        String text = request.get("text");
        String sessionId = request.get("sessionId");
        String identifier = sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;

        if (!rateLimiter.allowRequest(identifier)) {
            log.warn("레이트 리미팅 초과 - identifier: {}", identifier);
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.info("테스트 요청 - 텍스트 길이: {}글자, sessionId: {}", text.length(), sessionId);

        VoiceData voiceData = voiceService.processText(text, sessionId);

        return ResponseEntity.status(HttpStatus.OK).body(
            VoiceProcessResponse.success("테스트 요청이 성공적으로 처리되었습니다.", voiceData)
        );
    }

    @PostMapping(value = "/process-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> processVoiceStream(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "sessionId", required = false) String sessionId
    ) {
        String identifier = sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;

        if (!rateLimiter.allowRequest(identifier)) {
            log.warn("레이트 리미팅 초과 - identifier: {}", identifier);
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.info("음성 스트리밍 처리 요청 - 크기: {}bytes, sessionId: {}", file.getSize(), sessionId);
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onTimeout(() -> {
            log.warn("음성 스트리밍 타임아웃");
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("타임아웃 처리 실패", e);
            }
        });
        emitter.onError(e -> log.error("음성 스트리밍 에러", e));
        emitter.onCompletion(() -> log.info("음성 스트리밍 완료"));

        voiceService.processVoiceStreamAsync(file, sessionId, emitter);
        return ResponseEntity.ok(emitter);
    }

    @PostMapping(value = "/test-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> processTestStream(
        @RequestBody Map<String, String> request
    ) {
        if (request == null || request.get("text") == null) {
            throw new IllegalArgumentException("text 필드가 필요합니다.");
        }
        String text = request.get("text");
        String sessionId = request.get("sessionId");
        String identifier = sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;

        if (!rateLimiter.allowRequest(identifier)) {
            log.warn("레이트 리미팅 초과 - identifier: {}", identifier);
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.info("테스트 스트리밍 요청 - 텍스트 길이: {}글자, sessionId: {}", text.length(), sessionId);

        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onTimeout(() -> {
            log.warn("텍스트 스트리밍 타임아웃");
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("타임아웃 처리 실패", e);
            }
        });
        emitter.onError(e -> log.error("텍스트 스트리밍 에러", e));
        emitter.onCompletion(() -> log.info("텍스트 스트리밍 완료"));

        voiceService.processTextStreamAsync(text, sessionId, emitter);
        return ResponseEntity.ok(emitter);
    }
}