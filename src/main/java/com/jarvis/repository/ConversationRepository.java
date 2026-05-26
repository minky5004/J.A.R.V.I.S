package com.jarvis.repository;

import com.jarvis.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<Conversation> findBySessionIdAndCreatedAtAfter(String sessionId, LocalDateTime startDate);

    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
