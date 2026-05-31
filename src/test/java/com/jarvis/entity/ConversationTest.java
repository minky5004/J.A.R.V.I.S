package com.jarvis.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conversation Entity 테스트")
class ConversationTest {

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = new Conversation();
    }

    @Test
    @DisplayName("Conversation 생성")
    void testConversationCreation() {
        // When
        conversation.setSessionId("test-session");
        conversation.setMessage("안녕하세요");
        conversation.setRole(ConversationRole.USER);
        conversation.setCreatedAt(LocalDateTime.now());

        // Then
        assertNotNull(conversation);
        assertEquals("test-session", conversation.getSessionId());
        assertEquals("안녕하세요", conversation.getMessage());
        assertEquals(ConversationRole.USER, conversation.getRole());
        assertNotNull(conversation.getCreatedAt());
    }

    @Test
    @DisplayName("USER 역할 설정")
    void testUserRole() {
        // When
        conversation.setRole(ConversationRole.USER);

        // Then
        assertEquals(ConversationRole.USER, conversation.getRole());
    }

    @Test
    @DisplayName("ASSISTANT 역할 설정")
    void testAssistantRole() {
        // When
        conversation.setRole(ConversationRole.ASSISTANT);

        // Then
        assertEquals(ConversationRole.ASSISTANT, conversation.getRole());
    }

    @Test
    @DisplayName("메시지 저장")
    void testMessageStorage() {
        // Given
        String message = "이것은 테스트 메시지입니다.";

        // When
        conversation.setMessage(message);

        // Then
        assertEquals(message, conversation.getMessage());
    }

    @Test
    @DisplayName("세션 ID 저장")
    void testSessionIdStorage() {
        // Given
        String sessionId = "user-session-12345";

        // When
        conversation.setSessionId(sessionId);

        // Then
        assertEquals(sessionId, conversation.getSessionId());
    }

    @Test
    @DisplayName("생성 시간 기록")
    void testCreatedAtTimestamp() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        conversation.setCreatedAt(now);

        // Then
        assertEquals(now, conversation.getCreatedAt());
        assertNotNull(conversation.getCreatedAt());
    }

    @Test
    @DisplayName("완전한 Conversation 객체")
    void testCompleteConversation() {
        // When
        conversation.setSessionId("session-123");
        conversation.setMessage("안녕하세요, 날씨가 어때요?");
        conversation.setRole(ConversationRole.USER);
        conversation.setCreatedAt(LocalDateTime.now());

        // Then
        assertEquals("session-123", conversation.getSessionId());
        assertTrue(conversation.getMessage().contains("날씨"));
        assertEquals(ConversationRole.USER, conversation.getRole());
        assertNotNull(conversation.getCreatedAt());
    }

    @Test
    @DisplayName("다국어 메시지 지원")
    void testMultilingualMessages() {
        // When
        conversation.setMessage("これはテストです");
        Conversation conversation2 = new Conversation();
        conversation2.setMessage("这是一个测试");

        // Then
        assertEquals("これはテストです", conversation.getMessage());
        assertEquals("这是一个测试", conversation2.getMessage());
    }

    @Test
    @DisplayName("메시지 수정")
    void testMessageModification() {
        // Given
        conversation.setMessage("원래 메시지");

        // When
        conversation.setMessage("수정된 메시지");

        // Then
        assertEquals("수정된 메시지", conversation.getMessage());
    }
}
