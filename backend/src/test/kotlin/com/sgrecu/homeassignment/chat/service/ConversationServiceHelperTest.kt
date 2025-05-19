package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.repository.ConversationRepository
import com.sgrecu.homeassignment.chat.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.TransientDataAccessResourceException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals

@DisplayName("ConversationService Helper Methods Tests")
class ConversationServiceHelperTest {

    private lateinit var conversationRepository: ConversationRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var clock: Clock
    private lateinit var conversationService: ConversationService
    private val now = Instant.now()
    private val userId = "user-123"
    private val conversationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        conversationRepository = mockk()
        messageRepository = mockk()
        clock = Clock.fixed(now, ZoneId.systemDefault())

        conversationService = ConversationService(
            conversationRepository, messageRepository, clock
        )
    }

    @Test
    fun `handleConversationUpdateError returns empty for TransientDataAccessResourceException`() {
        // Given
        val exception = TransientDataAccessResourceException("Row with Id [$conversationId] does not exist")

        // When
        val result = conversationService.javaClass.getDeclaredMethod(
                "handleConversationUpdateError",
                Throwable::class.java,
                UUID::class.java
            ).apply { isAccessible = true }.invoke(conversationService, exception, conversationId)

        // Then
        StepVerifier.create(result as Mono<*>).verifyComplete()
    }

    @Test
    fun `handleConversationUpdateError propagates other errors`() {
        // Given
        val exception = RuntimeException("Some other error")

        // When
        val result = conversationService.javaClass.getDeclaredMethod(
                "handleConversationUpdateError",
                Throwable::class.java,
                UUID::class.java
            ).apply { isAccessible = true }.invoke(conversationService, exception, conversationId)

        // Then
        StepVerifier.create(result as Mono<*>).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `handleMessageSaveError returns specific error for foreign key violations`() {
        // Given
        val exception = DataIntegrityViolationException("FOREIGN KEY (CONVERSATION_ID) REFERENCES CONVERSATION")

        // When
        val result = conversationService.javaClass.getDeclaredMethod(
                "handleMessageSaveError",
                Throwable::class.java,
                UUID::class.java
            ).apply { isAccessible = true }.invoke(conversationService, exception, conversationId)

        // Then
        StepVerifier.create(result as Mono<*>).expectError(IllegalStateException::class.java).verify()
    }

    @Test
    fun `handleMessageSaveError propagates other errors`() {
        // Given
        val exception = RuntimeException("Some other error")

        // When
        val result = conversationService.javaClass.getDeclaredMethod(
                "handleMessageSaveError",
                Throwable::class.java,
                UUID::class.java
            ).apply { isAccessible = true }.invoke(conversationService, exception, conversationId)

        // Then
        StepVerifier.create(result as Mono<*>).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `addMessageInternal updates conversation timestamp and saves message`() {
        // Given
        val conversation = Conversation(
            id = conversationId,
            userId = userId,
            title = "Test Conversation",
            createdAt = now.minusSeconds(86400),
            updatedAt = now.minusSeconds(86400)
        )

        val updatedConversation = conversation.copy(updatedAt = now)
        val messageSlot = slot<Message>()
        val role = MessageRoleEnum.USER
        val content = "Hello, world!"

        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { conversationRepository.save(any()) } returns Mono.just(updatedConversation)
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When/Then
        StepVerifier.create(conversationService.addMessageInternal(conversationId, role, content))
            .expectNextMatches { message ->
                message.conversationId == conversationId && message.role == role && message.content == content && message.createdAt == now
            }.verifyComplete()

        verify { conversationRepository.save(match { it.updatedAt == now }) }
        assertEquals(conversationId, messageSlot.captured.conversationId)
        assertEquals(role, messageSlot.captured.role)
        assertEquals(content, messageSlot.captured.content)
        assertEquals(now, messageSlot.captured.createdAt)
    }

    @Test
    fun `handleMessageSaveError handles specific database error types`() {
        // Given
        val uniqueConstraintException = DataIntegrityViolationException("Unique constraint violation")

        // When
        val result = conversationService.javaClass.getDeclaredMethod(
                "handleMessageSaveError",
                Throwable::class.java,
                UUID::class.java
            ).apply { isAccessible = true }.invoke(conversationService, uniqueConstraintException, conversationId)

        // Then
        StepVerifier.create(result as Mono<*>).expectErrorMatches { it is RuntimeException }.verify()
    }
} 