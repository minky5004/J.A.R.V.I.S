package com.jarvis.service;

import com.jarvis.entity.Conversation;
import com.jarvis.entity.ConversationRole;
import com.jarvis.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation saveMessage(String sessionId, String message, ConversationRole role) {
        Conversation conversation = new Conversation(sessionId, message, role);
        Conversation saved = conversationRepository.save(conversation);
        log.info("대화 저장 - sessionId: {}, role: {}, 메시지 길이: {}글자",
            sessionId, role, message.length());
        return saved;
    }

    public List<Conversation> getConversationHistory(String sessionId) {
        List<Conversation> conversations = conversationRepository
            .findBySessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId);
        log.debug("대화 이력 조회 - sessionId: {}, 대화 수: {}",
            sessionId, conversations.size());
        return conversations;
    }

    public Page<Conversation> getConversationHistoryPaged(String sessionId, Pageable pageable) {
        List<Conversation> conversations = conversationRepository
            .findBySessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), conversations.size());
        List<Conversation> pageContent = conversations.subList(start, end);
        log.debug("페이지 조회 - sessionId: {}, page: {}, size: {}",
            sessionId, pageable.getPageNumber(), pageable.getPageSize());
        return new PageImpl<>(pageContent, pageable, conversations.size());
    }

    public List<Conversation> getRecentConversations(String sessionId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Conversation> conversations = conversationRepository
            .findBySessionIdAndIsDeletedFalseAndCreatedAtAfter(sessionId, thirtyDaysAgo);
        log.debug("최근 30일 대화 조회 - sessionId: {}, 대화 수: {}",
            sessionId, conversations.size());
        return conversations;
    }

    @Transactional
    public void deleteSessionHistory(String sessionId) {
        long deletedCount = conversationRepository.softDeleteBySessionId(sessionId);
        if (deletedCount > 0) {
            log.info("세션 대화 소프트 삭제 - sessionId: {}, 삭제된 건수: {}", sessionId, deletedCount);
        }
    }

    @Transactional
    public void hardDeleteSessionHistory(String sessionId) {
        long deletedCount = conversationRepository.deleteAllBySessionId(sessionId);
        if (deletedCount > 0) {
            log.info("세션 대화 완전 삭제 - sessionId: {}, 삭제된 건수: {}",
                sessionId, deletedCount);
        }
    }

    @Transactional
    @Scheduled(fixedRate = 86400000)
    public void cleanupOldConversations() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long deletedCount = conversationRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
        log.info("자동 정리 실행 - 30일 이상 된 대화 {}건 삭제", deletedCount);
    }
}
