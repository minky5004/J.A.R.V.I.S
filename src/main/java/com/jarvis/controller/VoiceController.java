package com.jarvis.controller;

import com.jarvis.dto.VoiceData;
import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.service.VoiceService;
import com.jarvis.util.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Voice API", description = "음성 처리 API - 음성 파일 업로드 및 STT, AI 응답, TTS 처리")
public class VoiceController {
    private final VoiceService voiceService;
    private final RateLimiter rateLimiter;

    @Operation(summary = "음성 파일 처리", description = "음성 파일을 STT로 변환하고 AI 의도 파악 후 결과를 TTS로 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @PostMapping("/process")
    public ResponseEntity<VoiceProcessResponse<VoiceData>> processVoice(
        @Parameter(description = "음성 파일 (WAV, MP3 등)", required = true)
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "세션 ID (선택사항, 대화 이력 관리용)")
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

    @Operation(summary = "텍스트 테스트 처리", description = "텍스트를 입력받아 AI 의도 파악 후 결과를 TTS로 반환 (음성 파일 필요 없음)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @PostMapping("/test")
    public ResponseEntity<VoiceProcessResponse<VoiceData>> processTest(
        @Parameter(description = "text: 처리할 텍스트, sessionId: 세션 ID (선택사항)")
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

    @Operation(summary = "음성 파일 스트리밍 처리", description = "음성 파일을 처리하고 결과를 Server-Sent Events로 스트리밍")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스트리밍 시작"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @PostMapping(value = "/process-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> processVoiceStream(
        @Parameter(description = "음성 파일 (WAV, MP3 등)", required = true)
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "세션 ID (선택사항)")
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

    @Operation(summary = "텍스트 스트리밍 처리", description = "텍스트를 처리하고 결과를 Server-Sent Events로 스트리밍")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스트리밍 시작"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @PostMapping(value = "/test-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> processTestStream(
        @Parameter(description = "text: 처리할 텍스트, sessionId: 세션 ID (선택사항)")
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