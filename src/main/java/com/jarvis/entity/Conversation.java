package com.jarvis.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Schema(description = "대화 이력")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "대화 ID", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "세션 ID", example = "user-12345")
    private String sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "메시지 내용", example = "내일 날씨가 어때?")
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "역할", example = "USER", allowableValues = {"USER", "ASSISTANT"})
    private ConversationRole role;

    @Column(nullable = false, updatable = false)
    @Schema(description = "생성 시간", example = "2026-05-30T10:30:00")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "삭제 여부", example = "false")
    private Boolean isDeleted = false;

    public Conversation() {
    }

    public Conversation(String sessionId, String message, ConversationRole role) {
        this.sessionId = sessionId;
        this.message = message;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ConversationRole getRole() {
        return role;
    }

    public void setRole(ConversationRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", messageLength=" + (message != null ? message.length() : 0) +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}
