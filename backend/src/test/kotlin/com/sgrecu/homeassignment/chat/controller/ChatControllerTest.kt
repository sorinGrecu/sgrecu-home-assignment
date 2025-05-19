package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.service.ChatCoordinator
import com.sgrecu.homeassignment.chat.util.createErrorEvent
import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.*

class ChatControllerTest {

    @Mock
    private lateinit var chatCoordinator: ChatCoordinator

    private lateinit var chatController: ChatController

    private val mockUserId = "test-user-123"
    private val mockUserPrincipal = UserPrincipal(
        externalId = mockUserId, email = "test@example.com", roles = setOf("USER")
    )

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        chatController = ChatController(chatCoordinator)
    }

    @Test
    fun `streamChat with message and no conversationId should return SSE stream`() {
        // Given
        val userQuery = "Hello AI"
        val conversationUuid = UUID.randomUUID()

        val chunk1 = ChatResponseChunk(conversationId = conversationUuid.toString(), content = "AI says:")
        val chunk2 = ChatResponseChunk(conversationId = conversationUuid.toString(), content = " Hello there!")
        val sseEvent1 = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk1).build()
        val sseEvent2 = ServerSentEvent.builder<ChatResponseChunk>().id("2").event("message").data(chunk2).build()

        Mockito.`when`(chatCoordinator.streamChat(userQuery, null, mockUserId))
            .thenReturn(Flux.just(sseEvent1, sseEvent2))

        // When
        val resultFlux = chatController.streamChat(userQuery, null, mockUserPrincipal)

        // Then
        StepVerifier.create(resultFlux).expectNext(sseEvent1).expectNext(sseEvent2).verifyComplete()
        Mockito.verify(chatCoordinator).streamChat(userQuery, null, mockUserId)
    }

    @Test
    fun `streamChat with message and conversationId should return SSE stream`() {
        // Given
        val userQuery = "Tell me more"
        val conversationId = UUID.randomUUID().toString()

        val chunk1 = ChatResponseChunk(conversationId = conversationId, content = "Okay,")
        val chunk2 = ChatResponseChunk(conversationId = conversationId, content = " what do you want to know?")
        val sseEvent1 = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk1).build()
        val sseEvent2 = ServerSentEvent.builder<ChatResponseChunk>().id("2").event("message").data(chunk2).build()

        Mockito.`when`(chatCoordinator.streamChat(userQuery, conversationId, mockUserId))
            .thenReturn(Flux.just(sseEvent1, sseEvent2))

        // When
        val resultFlux = chatController.streamChat(userQuery, conversationId, mockUserPrincipal)

        // Then
        StepVerifier.create(resultFlux).expectNext(sseEvent1).expectNext(sseEvent2).verifyComplete()

        Mockito.verify(chatCoordinator).streamChat(userQuery, conversationId, mockUserId)
    }

    @Test
    fun `streamChat when coordinator returns empty flux should return empty SSE stream`() {
        // Given
        val userQuery = "Anything new?"
        val conversationId = UUID.randomUUID().toString()

        Mockito.`when`(chatCoordinator.streamChat(userQuery, conversationId, mockUserId)).thenReturn(Flux.empty())

        // When
        val resultFlux = chatController.streamChat(userQuery, conversationId, mockUserPrincipal)

        // Then
        StepVerifier.create(resultFlux).verifyComplete()

        Mockito.verify(chatCoordinator).streamChat(userQuery, conversationId, mockUserId)
    }

    @Test
    fun `streamChat when coordinator returns an error event should propagate it`() {
        // Given
        val userQuery = "This might fail"
        val conversationId = UUID.randomUUID()
        val errorMessage = "A critical error occurred in AI"

        val errorSseEvent = createErrorEvent(conversationId, errorMessage)

        Mockito.`when`(chatCoordinator.streamChat(userQuery, null, mockUserId)).thenReturn(Flux.just(errorSseEvent))

        // When
        val resultFlux = chatController.streamChat(userQuery, null, mockUserPrincipal)

        // Then
        StepVerifier.create(resultFlux).expectNext(errorSseEvent).verifyComplete()

        Mockito.verify(chatCoordinator).streamChat(userQuery, null, mockUserId)
    }
} 