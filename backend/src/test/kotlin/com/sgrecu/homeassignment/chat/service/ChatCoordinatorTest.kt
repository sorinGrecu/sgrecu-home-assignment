package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.publisher.TestPublisher
import java.time.Duration
import java.util.*

@DisplayName("ChatCoordinator Tests")
class ChatCoordinatorTest {

    private lateinit var aiTransport: AITransport
    private lateinit var conversationService: ConversationService
    private lateinit var messagePersister: MessagePersister
    private lateinit var chatCoordinator: ChatCoordinator

    private val userId = "user-123"
    private val userQuery = "Hello, AI!"
    private val title = "Hello, AI!"
    private val existingConversationId = UUID.randomUUID()
    private val newConversationId = UUID.randomUUID()
    private val conversation = Conversation(
        id = existingConversationId,
        userId = userId,
        title = title,
        createdAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now()
    )
    private val newConversation = conversation.copy(id = newConversationId)

    @BeforeEach
    fun setup() {
        aiTransport = mockk()
        conversationService = mockk()
        messagePersister = mockk()
        chatCoordinator = ChatCoordinator(aiTransport, conversationService, messagePersister)

        StepVerifier.setDefaultTimeout(Duration.ofSeconds(10))
    }

    @Test
    fun `streamChat creates new conversation when conversationUuid is null`() {
        // Given
        every { conversationService.findOrCreateConversation(null, userId, title) } returns Mono.just(newConversation)
        every {
            conversationService.addMessageInternal(
                newConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responseContent = listOf("Hello", ", ", "world", "!")
        val responsePublisher = TestPublisher.createCold<String>()

        every {
            aiTransport.createFilteredResponseStream(
                userQuery, newConversationId
            )
        } returns responsePublisher.flux()
        every { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, null, userId)

        val verifier = StepVerifier.create(result)

        responsePublisher.emit(responseContent[0])

        verifier.expectNextMatches { event ->
            event.event() == "message" && event.id() == "1" && event.data()?.conversationId == newConversationId.toString() && event.data()?.content == "Hello"
        }

        responsePublisher.emit(responseContent[1])
        responsePublisher.emit(responseContent[2])
        responsePublisher.emit(responseContent[3])

        verifier.expectNextCount(3).then { responsePublisher.complete() }.verifyComplete()

        verify { conversationService.addMessageInternal(newConversationId, MessageRoleEnum.USER, userQuery) }
        verify { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) }
    }

    @Test
    fun `streamChat creates new conversation when user mismatch on existing conversation`() {
        // Given
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            newConversation
        )
        every {
            conversationService.addMessageInternal(
                newConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.create<String>()
        every {
            aiTransport.createFilteredResponseStream(
                userQuery, newConversationId
            )
        } returns responsePublisher.flux()
        every { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).then { responsePublisher.emit("Hello") }.expectNextCount(1)
            .then { responsePublisher.complete() }.verifyComplete()

        verify { conversationService.addMessageInternal(newConversationId, MessageRoleEnum.USER, userQuery) }
        verify { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) }
    }

    @Test
    fun `streamChat handles invalid UUID format by creating new conversation`() {
        // Given
        val invalidUuid = "not-a-uuid"

        every { conversationService.findOrCreateConversation(null, userId, title) } returns Mono.just(newConversation)
        every {
            conversationService.addMessageInternal(
                newConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.create<String>()
        every {
            aiTransport.createFilteredResponseStream(
                userQuery, newConversationId
            )
        } returns responsePublisher.flux()
        every { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, invalidUuid, userId)

        // Then
        StepVerifier.create(result).then { responsePublisher.emit("Response") }.expectNextCount(1)
            .then { responsePublisher.complete() }.verifyComplete()

        verify { conversationService.findOrCreateConversation(null, userId, title) }
    }

    @Test
    fun `streamChat handles AI failure by returning error SSE`() {
        // Given
        val errorMessage = "AI model failed to generate response"
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        every {
            aiTransport.createFilteredResponseStream(
                userQuery, existingConversationId
            )
        } throws AIResponseFailedException(errorMessage)

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).expectNextMatches { event ->
            event.event() == "error" && event.id() == "error" && event.data()?.conversationId == existingConversationId.toString() && event.data()?.content?.contains(
                errorMessage
            ) == true
        }.verifyComplete()
    }

    @Test
    fun `streamChat handles other exceptions and propagates them after error SSE`() {
        // Given
        val exception = RuntimeException("Unexpected error")
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        every { aiTransport.createFilteredResponseStream(userQuery, existingConversationId) } throws exception

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).expectNextMatches { event ->
            event.event() == "error" && event.id() == "error" && event.data()?.conversationId == existingConversationId.toString() && event.data()?.content?.contains(
                exception.message!!
            ) == true
        }.expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `streamChat verifies SSE IDs increment correctly`() {
        // Given
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.createCold<String>()
        every {
            aiTransport.createFilteredResponseStream(
                userQuery, existingConversationId
            )
        } returns responsePublisher.flux()
        every {
            messagePersister.saveMessage(
                any(), existingConversationId, MessageRoleEnum.ASSISTANT
            )
        } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        val verifier = StepVerifier.create(result)

        responsePublisher.emit("First")

        verifier.expectNextMatches { event -> event.id() == "1" }

        responsePublisher.emit("Second")

        verifier.expectNextMatches { event -> event.id() == "2" }

        responsePublisher.emit("Third")

        verifier.expectNextMatches { event -> event.id() == "3" }.then { responsePublisher.complete() }.verifyComplete()
    }

    @Test
    fun `streamChat handles exception in findOrCreateConversation`() {
        // Given
        val errorMessage = "Database error"
        val exception = IllegalStateException(errorMessage)

        every {
            conversationService.findOrCreateConversation(
                existingConversationId, userId, title
            )
        } returns Mono.error(exception)

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
    }

    @Test
    fun `streamChat handles exception in addMessageInternal`() {
        // Given
        val errorMessage = "Failed to save message"
        val exception = IllegalStateException(errorMessage)

        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.error(exception)

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).expectErrorMatches { error ->
            error is IllegalStateException && error.message == errorMessage
        }.verify()
    }

    @Test
    fun `streamChat handles error in message persistence`() {
        // Given
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.create<String>()
        every {
            aiTransport.createFilteredResponseStream(
                userQuery, existingConversationId
            )
        } returns responsePublisher.flux()

        val persistenceError = RuntimeException("Failed to persist message")
        every {
            messagePersister.saveMessage(
                any(), existingConversationId, MessageRoleEnum.ASSISTANT
            )
        } returns Mono.error(persistenceError)

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result).then { responsePublisher.emit("Response") }.expectNextCount(1)
            .then { responsePublisher.complete() }.verifyComplete()

        verify { messagePersister.saveMessage(any(), existingConversationId, MessageRoleEnum.ASSISTANT) }
    }

    @Test
    fun `streamChat handles empty conversationUuid string`() {
        // Given
        val emptyUuid = "   "

        every { conversationService.findOrCreateConversation(null, userId, title) } returns Mono.just(newConversation)
        every {
            conversationService.addMessageInternal(
                newConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.create<String>()
        every {
            aiTransport.createFilteredResponseStream(
                userQuery, newConversationId
            )
        } returns responsePublisher.flux()
        every { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, emptyUuid, userId)

        // Then
        StepVerifier.create(result).then { responsePublisher.emit("Response") }.expectNextCount(1)
            .then { responsePublisher.complete() }.verifyComplete()

        verify { conversationService.findOrCreateConversation(null, userId, title) }
    }

    @Test
    fun `streamChat handles very long userQuery by truncating title`() {
        // Given
        val longQuery = "A".repeat(100)
        val truncatedTitle = "A".repeat(30) + "..."

        every { conversationService.findOrCreateConversation(null, userId, truncatedTitle) } returns Mono.just(
            newConversation
        )
        every {
            conversationService.addMessageInternal(
                newConversationId, MessageRoleEnum.USER, longQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.create<String>()
        every {
            aiTransport.createFilteredResponseStream(
                longQuery, newConversationId
            )
        } returns responsePublisher.flux()
        every { messagePersister.saveMessage(any(), newConversationId, MessageRoleEnum.ASSISTANT) } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(longQuery, null, userId)

        // Then
        StepVerifier.create(result).then { responsePublisher.emit("Response") }.expectNextCount(1)
            .then { responsePublisher.complete() }.verifyComplete()

        verify { conversationService.findOrCreateConversation(null, userId, truncatedTitle) }
    }

    @Test
    fun `streamChat cancels upstream when client disconnects`() {
        // Given
        every { conversationService.findOrCreateConversation(existingConversationId, userId, title) } returns Mono.just(
            conversation
        )
        every {
            conversationService.addMessageInternal(
                existingConversationId, MessageRoleEnum.USER, userQuery
            )
        } returns Mono.empty()

        val responsePublisher = TestPublisher.createCold<String>()

        every {
            aiTransport.createFilteredResponseStream(
                userQuery, existingConversationId
            )
        } returns responsePublisher.flux()
        every {
            messagePersister.saveMessage(
                any(), existingConversationId, MessageRoleEnum.ASSISTANT
            )
        } returns Mono.empty()

        // When
        val result = chatCoordinator.streamChat(userQuery, existingConversationId.toString(), userId)

        // Then
        StepVerifier.create(result.take(1)).then { responsePublisher.emit("First chunk") }.expectNextCount(1)
            .thenCancel().verify()

        StepVerifier.create(responsePublisher.flux()).expectSubscription().expectNoEvent(Duration.ofMillis(100))
            .thenCancel().verify()
    }
} 