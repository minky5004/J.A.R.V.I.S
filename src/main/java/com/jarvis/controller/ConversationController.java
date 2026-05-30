package com.jarvis.controller;

import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.entity.Conversation;
import com.jarvis.service.ConversationService;
import com.jarvis.util.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
@Tag(name = "Conversation API", description = "대화 이력 관리 API - 세션별 메시지 저장 및 조회")
public class ConversationController {

    private final ConversationService conversationService;
    private final RateLimiter rateLimiter;

    @Operation(summary = "대화 이력 조회 (페이지네이션)", description = "세션 ID로 대화 이력을 페이지별로 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 sessionId")
    })
    @GetMapping("/{sessionId}")
    public ResponseEntity<VoiceProcessResponse<Page<Conversation>>> getHistory(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable String sessionId,
            @Parameter(description = "페이지 정보 (기본값: 0페이지, 20개)")
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("유효하지 않은 sessionId 요청");
            return ResponseEntity.badRequest()
                .body(VoiceProcessResponse.failure("유효한 sessionId가 필요합니다."));
        }
        log.info("대화 이력 조회 요청 - sessionId: {}, page: {}, size: {}",
            sessionId, pageable.getPageNumber(), pageable.getPageSize());
        Page<Conversation> history = conversationService.getConversationHistoryPaged(sessionId, pageable);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "대화 이력을 불러왔습니다.",
            history
        ));
    }

    @Operation(summary = "최근 30일 대화 조회", description = "세션 ID로 최근 30일 동안의 모든 대화 메시지 조회 (페이지네이션 없음)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 sessionId")
    })
    @GetMapping("/{sessionId}/recent")
    public ResponseEntity<VoiceProcessResponse<List<Conversation>>> getRecent(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("유효하지 않은 sessionId 요청");
            return ResponseEntity.badRequest()
                .body(VoiceProcessResponse.failure("유효한 sessionId가 필요합니다."));
        }
        log.info("최근 30일 대화 조회 요청 - sessionId: {}", sessionId);
        List<Conversation> recentConversations = conversationService.getRecentConversations(sessionId);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "최근 30일 대화를 불러왔습니다.",
            recentConversations
        ));
    }

    @Operation(summary = "세션 대화 삭제", description = "해당 세션 ID의 모든 대화 이력 삭제 (분당 10회 제한)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 sessionId"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<VoiceProcessResponse<Void>> deleteHistory(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("유효하지 않은 sessionId 요청");
            return ResponseEntity.badRequest()
                .body(VoiceProcessResponse.failure("유효한 sessionId가 필요합니다."));
        }

        if (!rateLimiter.allowRequest(sessionId)) {
            log.warn("레이트 리미팅 초과 - DELETE 요청, sessionId: {}", sessionId);
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.info("세션 대화 삭제 요청 - sessionId: {}", sessionId);
        conversationService.deleteSessionHistory(sessionId);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "대화 이력을 삭제했습니다.",
            null
        ));
    }

    @Operation(summary = "30일 이상 된 대화 자동 정리", description = "30일 이상 된 모든 대화 이력 자동 삭제 (관리자 권한 필요 - Phase 8-4에서 @PreAuthorize 추가 예정)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "정리 성공"),
        @ApiResponse(responseCode = "429", description = "레이트 리미팅 초과")
    })
    @PostMapping("/cleanup")
    public ResponseEntity<VoiceProcessResponse<String>> cleanup() {
        String identifier = "admin-cleanup";

        if (!rateLimiter.allowRequest(identifier)) {
            log.warn("레이트 리미팅 초과 - 자동 정리 요청");
            return ResponseEntity.status(429).body(
                VoiceProcessResponse.failure("요청 횟수가 초과되었습니다. 분당 10회만 가능합니다.")
            );
        }

        log.warn("자동 정리 요청 - 관리자 권한 검증 필요 (Phase 8-4에서 @PreAuthorize 추가 예정)");
        conversationService.cleanupOldConversations();
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "자동 정리가 실행되었습니다.",
            "완료"
        ));
    }
}
