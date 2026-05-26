package com.jarvis.service;

import com.jarvis.entity.Conversation;
import com.jarvis.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation saveMessage(String sessionId, String message, String role) {
        Conversation conversation = new Conversation(sessionId, message, role);
        Conversation saved = conversationRepository.save(conversation);
        log.info("대화 저장 - sessionId: {}, role: {}, 메시지 길이: {}글자",
            sessionId, role, message.length());
        return saved;
    }

    public List<Conversation> getConversationHistory(String sessionId) {
        List<Conversation> conversations = conversationRepository
            .findBySessionIdOrderByCreatedAtDesc(sessionId);
        log.debug("대화 이력 조회 - sessionId: {}, 대화 수: {}",
            sessionId, conversations.size());
        return conversations;
    }

    public List<Conversation> getRecentConversations(String sessionId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Conversation> conversations = conversationRepository
            .findBySessionIdAndCreatedAtAfter(sessionId, thirtyDaysAgo);
        log.debug("최근 30일 대화 조회 - sessionId: {}, 대화 수: {}",
            sessionId, conversations.size());
        return conversations;
    }

    public void deleteSessionHistory(String sessionId) {
        List<Conversation> conversations = conversationRepository
            .findBySessionIdOrderByCreatedAtDesc(sessionId);
        if (!conversations.isEmpty()) {
            conversationRepository.deleteAll(conversations);
            log.info("세션 대화 삭제 - sessionId: {}, 삭제된 건수: {}",
                sessionId, conversations.size());
        }
    }

    @Scheduled(fixedRate = 86400000)
    public void cleanupOldConversations() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long deletedCount = conversationRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
        log.info("자동 정리 실행 - 30일 이상 된 대화 {}건 삭제", deletedCount);
    }
}
