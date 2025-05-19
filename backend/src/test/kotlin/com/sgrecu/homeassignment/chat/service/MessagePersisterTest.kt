package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.strategy.MessageSaveStrategy
import com.sgrecu.homeassignment.metrics.MetricsService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.dao.DataIntegrityViolationException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.*

@DisplayName("MessagePersister Tests")
class MessagePersisterTest {

    private lateinit var saveStrategy: MessageSaveStrategy
    private lateinit var metricsService: MetricsService
    private lateinit var messagePersister: MessagePersister
    private val conversationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        saveStrategy = mockk()
        metricsService = mockk(relaxUnitFun = true)
        messagePersister = MessagePersister(saveStrategy, metricsService)
    }

    @Test
    fun `saveMessage delegates to configured save strategy`() {
        // Given
        val content = Flux.just("test", "message")
        val role = MessageRoleEnum.USER

        every { saveStrategy.save(content, conversationId, role) } returns Mono.empty()

        // When
        val result = messagePersister.saveMessage(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { saveStrategy.save(content, conversationId, role) }
        verify(exactly = 0) { metricsService.recordMessagePersistenceFailure(any(), any(), any()) }
    }

    @Test
    fun `saveMessage records metrics when strategy save fails`() {
        // Given
        val content = Flux.just("test", "message")
        val role = MessageRoleEnum.ASSISTANT
        val exception = RuntimeException("Save error")

        every { saveStrategy.save(content, conversationId, role) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(messagePersister.saveMessage(content, conversationId, role)).verifyComplete()

        verify { saveStrategy.save(content, conversationId, role) }
        verify { metricsService.recordMessagePersistenceFailure(conversationId, role, exception) }
    }

    @Test
    fun `saveMessage gracefully handles errors from save strategy`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.USER
        val exception = RuntimeException("Database error")

        every { saveStrategy.save(content, conversationId, role) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(messagePersister.saveMessage(content, conversationId, role)).verifyComplete()

        verify { metricsService.recordMessagePersistenceFailure(conversationId, role, exception) }
    }

    @ParameterizedTest
    @EnumSource(MessageRoleEnum::class)
    fun `saveMessage works with all message roles`(role: MessageRoleEnum) {
        // Given
        val content = Flux.just("test")

        every { saveStrategy.save(content, conversationId, role) } returns Mono.empty()

        // When
        val result = messagePersister.saveMessage(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { saveStrategy.save(content, conversationId, role) }
    }

    @Test
    fun `saveMessage handles different types of exceptions`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.USER

        val exceptionTypes = listOf(
            RuntimeException("Runtime error"),
            IllegalArgumentException("Invalid argument"),
            NullPointerException("Null pointer"),
            DataIntegrityViolationException("Database constraint violation")
        )

        for (exception in exceptionTypes) {
            every { saveStrategy.save(content, conversationId, role) } returns Mono.error(exception)

            // When/Then
            StepVerifier.create(messagePersister.saveMessage(content, conversationId, role)).verifyComplete()

            verify { metricsService.recordMessagePersistenceFailure(conversationId, role, exception) }
        }
    }

    @Test
    fun `saveMessage handles empty flux content`() {
        // Given
        val emptyContent = Flux.empty<String>()
        val role = MessageRoleEnum.ASSISTANT

        every { saveStrategy.save(emptyContent, conversationId, role) } returns Mono.empty()

        // When
        val result = messagePersister.saveMessage(emptyContent, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { saveStrategy.save(emptyContent, conversationId, role) }
    }

    @Test
    fun `saveMessage handles large content streams`() {
        // Given
        val largeContent = Flux.fromIterable((1..1000).map { "token$it" })
        val role = MessageRoleEnum.ASSISTANT

        every { saveStrategy.save(largeContent, conversationId, role) } returns Mono.empty()

        // When
        val result = messagePersister.saveMessage(largeContent, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { saveStrategy.save(largeContent, conversationId, role) }
    }

    @Test
    fun `saveMessage logs errors but still completes successfully`() {
        // Given
        val content = Flux.just("test content")
        val role = MessageRoleEnum.USER
        val exception = RuntimeException("Persistence error")

        every { saveStrategy.save(content, conversationId, role) } returns Mono.error(exception)

        // When
        val result = messagePersister.saveMessage(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()
        verify { metricsService.recordMessagePersistenceFailure(conversationId, role, exception) }
    }

    @Test
    fun `saveMessage preserves stream subscription context`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.ASSISTANT

        every { saveStrategy.save(content, conversationId, role) } returns Mono.empty()

        // When
        val result =
            messagePersister.saveMessage(content, conversationId, role).contextWrite { it.put("testKey", "testValue") }

        // Then
        StepVerifier.create(result).expectAccessibleContext().contains("testKey", "testValue").then().verifyComplete()
    }

    @Test
    fun `saveMessage correctly updates metrics for all error types`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.USER

        val dbException = DataIntegrityViolationException("DB constraint violation")
        every { saveStrategy.save(content, conversationId, role) } returns Mono.error(dbException)

        // When/Then
        StepVerifier.create(messagePersister.saveMessage(content, conversationId, role)).verifyComplete()

        // Verify specific metrics recording
        verify { metricsService.recordMessagePersistenceFailure(conversationId, role, dbException) }

        clearMocks(saveStrategy, metricsService)
        every { metricsService.recordMessagePersistenceFailure(any(), any(), any()) } just Runs

        val logicException = IllegalArgumentException("Invalid message state")
        every { saveStrategy.save(content, conversationId, role) } returns Mono.error(logicException)

        // When/Then
        StepVerifier.create(messagePersister.saveMessage(content, conversationId, role)).verifyComplete()

        verify { metricsService.recordMessagePersistenceFailure(conversationId, role, logicException) }
    }
} 