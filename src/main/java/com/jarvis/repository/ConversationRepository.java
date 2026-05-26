package com.jarvis.repository;

import com.jarvis.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<Conversation> findBySessionIdAndCreatedAtAfter(String sessionId, LocalDateTime startDate);

    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM Conversation c WHERE c.sessionId = :sessionId")
    long deleteAllBySessionId(String sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE Conversation c SET c.isDeleted = true WHERE c.sessionId = :sessionId")
    long softDeleteBySessionId(String sessionId);

    List<Conversation> findBySessionIdAndIsDeletedFalseOrderByCreatedAtDesc(String sessionId);

    List<Conversation> findBySessionIdAndIsDeletedFalseAndCreatedAtAfter(String sessionId, LocalDateTime startDate);
}
