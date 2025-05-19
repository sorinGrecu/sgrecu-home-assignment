package com.sgrecu.homeassignment.chat.strategy

import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.repository.MessageRepository
import com.sgrecu.homeassignment.chat.service.ContentFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

@DisplayName("EndOfStreamMessageSaveStrategy Tests")
class EndOfStreamMessageSaveStrategyTest {

    private lateinit var messageRepository: MessageRepository
    private lateinit var contentFilter: ContentFilter
    private lateinit var saveStrategy: EndOfStreamMessageSaveStrategy
    private val conversationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        messageRepository = mockk()
        contentFilter = mockk()
        saveStrategy = EndOfStreamMessageSaveStrategy(messageRepository, contentFilter)
    }

    @Test
    fun `save collects stream content and saves single message when complete`() {
        // Given
        val content = Flux.just("h", "i")
        val role = MessageRoleEnum.ASSISTANT
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(content) } returns content
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify(exactly = 1) { messageRepository.save(any()) }
        assertEquals("hi", messageSlot.captured.content)
        assertEquals(conversationId, messageSlot.captured.conversationId)
        assertEquals(role, messageSlot.captured.role)
    }

    @Test
    fun `save returns error when repository save fails`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.USER
        val exception = RuntimeException("Database error")

        every { contentFilter.filterContent(content) } returns content
        every { messageRepository.save(any()) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(saveStrategy.save(content, conversationId, role)).expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `save doesn't call repository when filtered content is empty`() {
        // Given
        val content = Flux.just("<think>", "hidden", "</think>")
        val role = MessageRoleEnum.ASSISTANT

        every { contentFilter.filterContent(content) } returns Flux.empty()

        // When
        val result = saveStrategy.save(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify(exactly = 0) { messageRepository.save(any()) }
    }

    @Test
    fun `getName returns correct strategy name`() {
        assertEquals("end-of-stream", saveStrategy.getName())
    }

    @Test
    fun `save can handle large content streams`() {
        // Given
        val largeContentChunks = (1..1000).map { "chunk$it" }
        val content = Flux.fromIterable(largeContentChunks)
        val role = MessageRoleEnum.USER
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(content) } returns content
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify(exactly = 1) { messageRepository.save(any()) }
        assertEquals(largeContentChunks.joinToString(""), messageSlot.captured.content)
        assertEquals(conversationId, messageSlot.captured.conversationId)
        assertEquals(role, messageSlot.captured.role)
    }

    @ParameterizedTest
    @EnumSource(MessageRoleEnum::class)
    fun `save works with all message roles`(role: MessageRoleEnum) {
        // Given
        val content = Flux.just("Content for $role")
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(content) } returns content
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(content, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { messageRepository.save(any()) }
        assertEquals(role, messageSlot.captured.role)
    }

    @Test
    fun `save handles special characters in content`() {
        // Given
        val specialContent = Flux.just("Line 1\nLine 2", "Special chars: áéíóú", "Symbols: @#$%^&*()")
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(specialContent) } returns specialContent
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(specialContent, conversationId, MessageRoleEnum.ASSISTANT)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { messageRepository.save(any()) }
        assertEquals(
            "Line 1\nLine 2Special chars: áéíóúSymbols: @#$%^&*()", messageSlot.captured.content
        )
    }

    @Test
    fun `save correctly handles delayed emissions in stream`() {
        // Given
        val delayedContent = Flux.just("first", "second", "third").delayElements(Duration.ofMillis(10))
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(delayedContent) } returns delayedContent
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(delayedContent, conversationId, MessageRoleEnum.USER)

        // Then
        StepVerifier.create(result).expectSubscription().verifyComplete()

        Thread.sleep(100)

        verify { messageRepository.save(any()) }
        assertEquals("firstsecondthird", messageSlot.captured.content)
    }

    @Test
    fun `save handles error during content filtering`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.ASSISTANT
        val filterException = IllegalStateException("Filter error")

        every { contentFilter.filterContent(content) } returns Flux.error(filterException)

        // When/Then
        StepVerifier.create(saveStrategy.save(content, conversationId, role))
            .expectError(IllegalStateException::class.java).verify()

        verify(exactly = 0) { messageRepository.save(any()) }
    }

    @Test
    fun `save delegates to content filter`() {
        // Given
        val content = Flux.just("test")
        val role = MessageRoleEnum.USER

        every { contentFilter.filterContent(content) } returns Flux.just("filtered")
        every { messageRepository.save(any()) } returns Mono.just(
            Message(
                id = 1L, conversationId = conversationId, role = role, content = "filtered"
            )
        )

        // When
        saveStrategy.save(content, conversationId, role).block()

        // Then
        verify(exactly = 1) { contentFilter.filterContent(content) }
    }

    @Test
    fun `save handles empty stream by not saving anything`() {
        // Given
        val emptyContent = Flux.empty<String>()
        val role = MessageRoleEnum.ASSISTANT

        every { contentFilter.filterContent(emptyContent) } returns emptyContent

        // When
        val result = saveStrategy.save(emptyContent, conversationId, role)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify(exactly = 0) { messageRepository.save(any()) }
    }

    @Test
    fun `save correctly handles extremely large content without overflow`() {
        // Given
        val largeContent = Flux.fromIterable((1..5000).map { "token$it" })
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(largeContent) } returns largeContent
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(largeContent, conversationId, MessageRoleEnum.ASSISTANT)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { messageRepository.save(any()) }

        val expectedLength = (1..5000).sumOf { "token$it".length }
        assertEquals(expectedLength, messageSlot.captured.content.length)
    }

    @Test
    fun `save correctly handles Unicode and emoji content`() {
        // Given
        val unicodeContent = Flux.just(
            "Hello", " 😀", " 🌍", " Unicode: ", "\u2022 Bullet point", "\u2764 Heart"
        )
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(unicodeContent) } returns unicodeContent
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(unicodeContent, conversationId, MessageRoleEnum.USER)

        // Then
        StepVerifier.create(result).verifyComplete()

        verify { messageRepository.save(any()) }

        assertEquals(
            "Hello 😀 🌍 Unicode: \u2022 Bullet point\u2764 Heart", messageSlot.captured.content
        )
    }

    @Test
    fun `save handles content with varying delays between emissions`() {
        // Given
        val variableDelayContent = Flux.concat(
            Flux.just("First part").delayElements(Duration.ofMillis(5)),
            Flux.just("Second part").delayElements(Duration.ofMillis(15)),
            Flux.just("Third part").delayElements(Duration.ofMillis(10))
        )
        val messageSlot = slot<Message>()

        every { contentFilter.filterContent(variableDelayContent) } returns variableDelayContent
        every { messageRepository.save(capture(messageSlot)) } answers {
            Mono.just(messageSlot.captured.copy(id = 1L))
        }

        // When
        val result = saveStrategy.save(variableDelayContent, conversationId, MessageRoleEnum.ASSISTANT)

        // Then
        StepVerifier.create(result).verifyComplete()

        Thread.sleep(100)

        verify { messageRepository.save(any()) }
        assertEquals("First partSecond partThird part", messageSlot.captured.content)
    }
} 