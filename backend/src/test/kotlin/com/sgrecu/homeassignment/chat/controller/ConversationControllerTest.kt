package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.service.ConversationService
import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.*

class ConversationControllerTest {

    private lateinit var conversationService: ConversationService
    private lateinit var conversationController: ConversationController

    private val mockUserId = "test-user-123"
    private val mockUserPrincipal = UserPrincipal(
        externalId = mockUserId, email = "test@example.com", roles = setOf("USER")
    )

    private val conversationId = UUID.randomUUID()
    private val title = "Test Conversation"
    private val updatedTitle = "Updated Conversation"
    private val now = Instant.now()

    private val testConversation = Conversation(
        id = conversationId, userId = mockUserId, title = title, createdAt = now.minusSeconds(3600), updatedAt = now
    )

    private val testMessage = Message(
        id = 1L,
        conversationId = conversationId,
        role = com.sgrecu.homeassignment.chat.model.MessageRoleEnum.USER,
        content = "Hello",
        createdAt = now
    )

    @BeforeEach
    fun setUp() {
        conversationService = Mockito.mock(ConversationService::class.java)
        conversationController = ConversationController(conversationService)
    }

    @Test
    fun `getUserConversations should call service and return results`() {
        // Given
        val conversations = listOf(testConversation, testConversation.copy(id = UUID.randomUUID()))
        whenever(conversationService.getUserConversations(eq(mockUserId))).thenReturn(Flux.fromIterable(conversations))

        // When
        val result = conversationController.getUserConversations(mockUserPrincipal)

        // Then
        StepVerifier.create(result).expectNextCount(2).verifyComplete()
    }

    @Test
    fun `getConversation should call service and return result`() {
        // Given
        whenever(conversationService.getConversation(eq(conversationId), eq(mockUserId))).thenReturn(
                Mono.just(
                    testConversation
                )
            )

        // When
        val result = conversationController.getConversation(conversationId, mockUserPrincipal)

        // Then
        StepVerifier.create(result).expectNext(testConversation).verifyComplete()
    }

    @Test
    fun `getConversationMessages should call service and return results`() {
        // Given
        val messages = listOf(testMessage, testMessage.copy(id = 2L))
        whenever(
            conversationService.getConversationMessages(
                eq(conversationId),
                eq(mockUserId)
            )
        ).thenReturn(Flux.fromIterable(messages))

        // When
        val result = conversationController.getConversationMessages(conversationId, mockUserPrincipal)

        // Then
        StepVerifier.create(result).expectNextCount(2).verifyComplete()
    }

    @Test
    fun `updateConversation should call service and return result`() {
        // Given
        val request = UpdateConversationRequest(updatedTitle)
        val updatedConversation = testConversation.copy(title = updatedTitle)
        whenever(
            conversationService.updateConversationTitle(
                eq(conversationId),
                eq(updatedTitle),
                eq(mockUserId)
            )
        ).thenReturn(Mono.just(updatedConversation))

        // When
        val result = conversationController.updateConversation(conversationId, request, mockUserPrincipal)

        // Then
        StepVerifier.create(result).expectNext(updatedConversation).verifyComplete()
    }

    @Test
    fun `deleteConversation should call service and return empty`() {
        // Given
        whenever(conversationService.deleteConversation(eq(conversationId), eq(mockUserId))).thenReturn(Mono.empty())

        // When
        val result = conversationController.deleteConversation(conversationId, mockUserPrincipal)

        // Then
        StepVerifier.create(result).verifyComplete()
    }
} 