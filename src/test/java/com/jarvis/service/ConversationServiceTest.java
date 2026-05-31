package com.jarvis.service;

import com.jarvis.entity.Conversation;
import com.jarvis.entity.ConversationRole;
import com.jarvis.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService 테스트")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    private String testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = "test-session-123";
    }

    @Test
    @DisplayName("대화 메시지 저장")
    void testSaveMessage() {
        // Given
        Conversation conversation = new Conversation();
        conversation.setSessionId(testSessionId);
        conversation.setMessage("테스트");
        conversation.setRole(ConversationRole.USER);

        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When
        Conversation result = conversationService.saveMessage(testSessionId, "테스트", ConversationRole.USER);

        // Then
        assertNotNull(result);
        assertEquals(testSessionId, result.getSessionId());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("AI 응답 메시지 저장")
    void testSaveAIMessage() {
        // Given
        Conversation aiMessage = new Conversation();
        aiMessage.setSessionId(testSessionId);
        aiMessage.setMessage("안녕하세요");
        aiMessage.setRole(ConversationRole.ASSISTANT);

        when(conversationRepository.save(any(Conversation.class))).thenReturn(aiMessage);

        // When
        Conversation result = conversationService.saveMessage(testSessionId, "안녕하세요", ConversationRole.ASSISTANT);

        // Then
        assertNotNull(result);
        assertEquals(ConversationRole.ASSISTANT, result.getRole());
    }

    @Test
    @DisplayName("대화 이력 조회")
    void testGetConversationHistory() {
        // Given
        List<Conversation> conversations = new ArrayList<>();
        Conversation c = new Conversation();
        c.setSessionId(testSessionId);
        c.setMessage("메시지");
        conversations.add(c);

        when(conversationRepository.findBySessionIdAndIsDeletedFalseOrderByCreatedAtDesc(anyString()))
                .thenReturn(conversations);

        // When
        List<Conversation> result = conversationService.getConversationHistory(testSessionId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("최근 30일 대화 조회")
    void testGetRecentConversations() {
        // Given
        List<Conversation> recentConversations = new ArrayList<>();
        Conversation c = new Conversation();
        c.setSessionId(testSessionId);
        c.setCreatedAt(LocalDateTime.now());
        recentConversations.add(c);

        when(conversationRepository.findBySessionIdAndIsDeletedFalseAndCreatedAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(recentConversations);

        // When
        List<Conversation> result = conversationService.getRecentConversations(testSessionId);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("세션 대화 소프트 삭제")
    void testDeleteSessionHistory() {
        // Given
        when(conversationRepository.softDeleteBySessionId(anyString())).thenReturn(3L);

        // When
        conversationService.deleteSessionHistory(testSessionId);

        // Then
        verify(conversationRepository).softDeleteBySessionId(testSessionId);
    }

    @Test
    @DisplayName("빈 메시지 리스트")
    void testEmptyMessageList() {
        // Given
        when(conversationRepository.findBySessionIdAndIsDeletedFalseOrderByCreatedAtDesc(anyString()))
                .thenReturn(new ArrayList<>());

        // When
        List<Conversation> result = conversationService.getConversationHistory(testSessionId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
