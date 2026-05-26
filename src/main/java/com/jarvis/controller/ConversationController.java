package com.jarvis.controller;

import com.jarvis.dto.VoiceProcessResponse;
import com.jarvis.entity.Conversation;
import com.jarvis.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<VoiceProcessResponse<List<Conversation>>> getHistory(
            @PathVariable String sessionId) {
        log.info("대화 이력 조회 요청 - sessionId: {}", sessionId);
        List<Conversation> history = conversationService.getConversationHistory(sessionId);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "대화 이력을 불러왔습니다.",
            history
        ));
    }

    @GetMapping("/{sessionId}/recent")
    public ResponseEntity<VoiceProcessResponse<List<Conversation>>> getRecent(
            @PathVariable String sessionId) {
        log.info("최근 30일 대화 조회 요청 - sessionId: {}", sessionId);
        List<Conversation> recentConversations = conversationService.getRecentConversations(sessionId);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "최근 30일 대화를 불러왔습니다.",
            recentConversations
        ));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<VoiceProcessResponse<Void>> deleteHistory(
            @PathVariable String sessionId) {
        log.info("세션 대화 삭제 요청 - sessionId: {}", sessionId);
        conversationService.deleteSessionHistory(sessionId);
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "대화 이력을 삭제했습니다.",
            null
        ));
    }

    @PostMapping("/cleanup")
    public ResponseEntity<VoiceProcessResponse<String>> cleanup() {
        log.info("자동 정리 요청");
        conversationService.cleanupOldConversations();
        return ResponseEntity.ok(VoiceProcessResponse.success(
            "자동 정리가 실행되었습니다.",
            "완료"
        ));
    }
}
