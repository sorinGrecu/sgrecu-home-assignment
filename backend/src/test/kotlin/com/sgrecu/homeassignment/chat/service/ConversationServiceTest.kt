package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.ConversationNotFoundException
import com.sgrecu.homeassignment.chat.exception.UnauthorizedConversationAccessException
import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.repository.ConversationRepository
import com.sgrecu.homeassignment.chat.repository.MessageRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.TransientDataAccessResourceException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

@DisplayName("ConversationService Public API Tests")
class ConversationServiceTest {

    private lateinit var conversationRepository: ConversationRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var clock: Clock
    private lateinit var conversationService: ConversationService

    private val now = Instant.now()
    private val userId = "user-123"
    private val otherUserId = "user-456"
    private val conversationId = UUID.randomUUID()
    private val title = "Test Conversation"

    private val conversation = Conversation(
        id = conversationId,
        userId = userId,
        title = title,
        createdAt = now.minusSeconds(3600),
        updatedAt = now.minusSeconds(1800)
    )

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
    fun `findOrCreateConversation creates new conversation when id is null`() {
        // Given
        val newConversation = conversation.copy(id = UUID.randomUUID())
        every { conversationRepository.save(any()) } returns Mono.just(newConversation)

        // When/Then
        StepVerifier.create(conversationService.findOrCreateConversation(null, userId, title))
            .expectNext(newConversation).verifyComplete()

        verify {
            conversationRepository.save(match {
                it.userId == userId && it.title == title && it.createdAt == now && it.updatedAt == now
            })
        }
    }

    @Test
    fun `findOrCreateConversation returns existing conversation when id exists and user matches`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)

        // When/Then
        StepVerifier.create(conversationService.findOrCreateConversation(conversationId, userId, title))
            .expectNext(conversation).verifyComplete()
    }

    @Test
    fun `findOrCreateConversation creates new conversation when user mismatch`() {
        // Given
        val newConversation = conversation.copy(id = UUID.randomUUID())
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation.copy(userId = otherUserId))
        every { conversationRepository.save(any()) } returns Mono.just(newConversation)

        // When/Then
        StepVerifier.create(conversationService.findOrCreateConversation(conversationId, userId, title))
            .expectNext(newConversation).verifyComplete()

        verify {
            conversationRepository.save(match {
                it.userId == userId && it.title == title && it.createdAt == now && it.updatedAt == now
            })
        }
    }

    @Test
    fun `findOrCreateConversation creates new conversation when conversation not found`() {
        // Given
        val newConversation = conversation.copy(id = UUID.randomUUID())
        every { conversationRepository.findById(conversationId) } returns Mono.empty()
        every { conversationRepository.save(any()) } returns Mono.just(newConversation)

        // When/Then
        StepVerifier.create(conversationService.findOrCreateConversation(conversationId, userId, title))
            .expectNext(newConversation).verifyComplete()

        verify {
            conversationRepository.save(match {
                it.userId == userId && it.title == title && it.createdAt == now && it.updatedAt == now
            })
        }
    }

    @Test
    fun `deleteConversation succeeds when user has access`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { messageRepository.deleteAllByConversationId(conversationId) } returns Mono.empty()
        every { conversationRepository.deleteById(conversationId) } returns Mono.empty()

        // When/Then
        StepVerifier.create(conversationService.deleteConversation(conversationId, userId)).verifyComplete()

        verify {
            conversationRepository.findById(conversationId)
            messageRepository.deleteAllByConversationId(conversationId)
            conversationRepository.deleteById(conversationId)
        }

        confirmVerified(conversationRepository, messageRepository)
    }

    @Test
    fun `deleteConversation throws ConversationNotFoundException when conversation not found`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.empty()

        // When/Then
        StepVerifier.create(conversationService.deleteConversation(conversationId, userId))
            .expectError(ConversationNotFoundException::class.java).verify()
    }

    @Test
    fun `deleteConversation throws UnauthorizedConversationAccessException when user mismatch`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation.copy(userId = otherUserId))

        // When/Then
        StepVerifier.create(conversationService.deleteConversation(conversationId, userId))
            .expectError(UnauthorizedConversationAccessException::class.java).verify()
    }

    @Test
    fun `createConversation creates a new conversation with default values`() {
        // Given
        val expectedConversation = Conversation(
            userId = userId, title = title, createdAt = now, updatedAt = now
        )

        every { conversationRepository.save(any()) } returns Mono.just(expectedConversation.copy(id = UUID.randomUUID()))

        // When/Then
        StepVerifier.create(conversationService.createConversation(userId, title)).expectNextMatches { conversation ->
            conversation.userId == userId && conversation.title == title && conversation.createdAt == now && conversation.updatedAt == now
        }.verifyComplete()

        verify { conversationRepository.save(any()) }
    }

    @Test
    fun `getUserConversations returns user conversations sorted by updatedAt`() {
        // Given
        val conversation1 = conversation.copy(id = UUID.randomUUID(), updatedAt = now.minusSeconds(1000))
        val conversation2 = conversation.copy(id = UUID.randomUUID(), updatedAt = now.minusSeconds(500))
        val conversation3 = conversation.copy(id = UUID.randomUUID(), updatedAt = now)

        every { conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId) } returns Flux.just(
            conversation3, conversation2, conversation1
        )

        // When/Then
        StepVerifier.create(conversationService.getUserConversations(userId)).expectNext(conversation3)
            .expectNext(conversation2).expectNext(conversation1).verifyComplete()

        verify { conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId) }
    }

    @Test
    fun `getConversation returns conversation when user has access`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)

        // When/Then
        StepVerifier.create(conversationService.getConversation(conversationId, userId)).expectNext(conversation)
            .verifyComplete()

        verify { conversationRepository.findById(conversationId) }
    }

    @Test
    fun `getConversation throws ConversationNotFoundException when conversation not found`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.empty()

        // When/Then
        StepVerifier.create(conversationService.getConversation(conversationId, userId))
            .expectError(ConversationNotFoundException::class.java).verify()

        verify { conversationRepository.findById(conversationId) }
    }

    @Test
    fun `getConversation throws UnauthorizedConversationAccessException when user mismatch`() {
        // Given
        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation.copy(userId = otherUserId))

        // When/Then
        StepVerifier.create(conversationService.getConversation(conversationId, userId))
            .expectError(UnauthorizedConversationAccessException::class.java).verify()

        verify { conversationRepository.findById(conversationId) }
    }

    @Test
    fun `updateConversationTitle updates title when user has access`() {
        // Given
        val newTitle = "Updated Title"
        val updatedConversation = conversation.copy(title = newTitle, updatedAt = now)

        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { conversationRepository.save(any()) } returns Mono.just(updatedConversation)

        // When/Then
        StepVerifier.create(conversationService.updateConversationTitle(conversationId, newTitle, userId))
            .expectNext(updatedConversation).verifyComplete()

        verify {
            conversationRepository.findById(conversationId)
            conversationRepository.save(match {
                it.id == conversationId && it.title == newTitle && it.updatedAt == now
            })
        }
    }

    @Test
    fun `addMessageInternal adds message to conversation and updates timestamp`() {
        // Given
        val content = "Test message"
        val role = MessageRoleEnum.USER

        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { conversationRepository.save(any()) } returns Mono.just(conversation.copy(updatedAt = now))
        every { messageRepository.save(any()) } answers {
            val message = firstArg<Message>()
            Mono.just(message.copy(id = 1L))
        }

        // When/Then
        StepVerifier.create(conversationService.addMessageInternal(conversationId, role, content))
            .expectNextMatches { message ->
                message.conversationId == conversationId && message.role == role && message.content == content && message.createdAt == now
            }.verifyComplete()

        verify {
            conversationRepository.findById(conversationId)
            conversationRepository.save(match { it.updatedAt == now })
            messageRepository.save(match {
                it.conversationId == conversationId && it.role == role && it.content == content && it.createdAt == now
            })
        }
    }

    @Test
    fun `addMessageInternal handles TransientDataAccessResourceException when conversation deleted`() {
        // Given
        val content = "Test message"
        val role = MessageRoleEnum.USER
        val errorMessage = "Row with Id [$conversationId] does not exist"

        every { conversationRepository.findById(conversationId) } returns Mono.error(
            TransientDataAccessResourceException(errorMessage)
        )
        every { messageRepository.save(any()) } answers {
            val message = firstArg<Message>()
            Mono.just(message.copy(id = 1L))
        }

        // When/Then
        StepVerifier.create(conversationService.addMessageInternal(conversationId, role, content)).expectNextCount(1)
            .verifyComplete()

        verify {
            conversationRepository.findById(conversationId)
            messageRepository.save(any())
        }
        verify(exactly = 0) { conversationRepository.save(any()) }
    }

    @Test
    fun `addMessageInternal propagates DataIntegrityViolationException from message save`() {
        // Given
        val content = "Test message"
        val role = MessageRoleEnum.USER
        val errorMessage = "FOREIGN KEY (CONVERSATION_ID) violation"

        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { conversationRepository.save(any()) } returns Mono.just(conversation.copy(updatedAt = now))
        every { messageRepository.save(any()) } returns Mono.error(DataIntegrityViolationException(errorMessage))

        // When/Then
        StepVerifier.create(conversationService.addMessageInternal(conversationId, role, content))
            .expectError(DataIntegrityViolationException::class.java).verify()
    }

    @Test
    fun `getConversationMessages returns messages for conversation when user has access`() {
        // Given
        val message1 = Message(
            id = 1,
            conversationId = conversationId,
            role = MessageRoleEnum.USER,
            content = "Hello",
            createdAt = now.minusSeconds(60)
        )
        val message2 = Message(
            id = 2,
            conversationId = conversationId,
            role = MessageRoleEnum.ASSISTANT,
            content = "Hi there",
            createdAt = now
        )

        every { conversationRepository.findById(conversationId) } returns Mono.just(conversation)
        every { messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId) } returns Flux.just(
            message1, message2
        )

        // When/Then
        StepVerifier.create(conversationService.getConversationMessages(conversationId, userId)).expectNext(message1)
            .expectNext(message2).verifyComplete()

        verify {
            conversationRepository.findById(conversationId)
            messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        }
    }

    @Test
    fun `deleteConversationInternal removes all messages and conversation`() {
        // Given
        every { messageRepository.deleteAllByConversationId(conversationId) } returns Mono.empty()
        every { conversationRepository.deleteById(conversationId) } returns Mono.empty()

        // When/Then
        StepVerifier.create(conversationService.deleteConversationInternal(conversationId)).verifyComplete()

        verify {
            messageRepository.deleteAllByConversationId(conversationId)
            conversationRepository.deleteById(conversationId)
        }
    }
} 