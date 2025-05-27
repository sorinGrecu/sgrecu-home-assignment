package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.service.ChatCoordinator
import com.sgrecu.homeassignment.chat.util.createErrorEvent
import com.sgrecu.homeassignment.config.AppProperties
import com.sgrecu.homeassignment.config.TestConfig
import com.sgrecu.homeassignment.security.config.TestSecurityConfig
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.oauth.GoogleAuthProperties
import com.sgrecu.homeassignment.security.util.WithMockUserPrincipal
import com.sgrecu.homeassignment.security.util.WithMockUserPrincipalSecurityContextFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import reactor.core.publisher.Flux
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val MOCK_USER_EXTERNAL_ID = "test-user-external-id-123"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(
    TestSecurityConfig::class,
    WithMockUserPrincipalSecurityContextFactory::class,
    ChatControllerIntegrationTest.TestMockBeansConfig::class,
    TestConfig::class
)
class ChatControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties::class, GoogleAuthProperties::class, AppProperties::class)
    internal class TestMockBeansConfig {
        val chatCoordinatorMock = mockk<ChatCoordinator>(relaxed = true)

        @Bean
        fun chatCoordinator(): ChatCoordinator {
            return chatCoordinatorMock
        }
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var testMockBeansConfig: TestMockBeansConfig

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat with message and no conversationId should return SSE stream`() {
        val userQuery = "Hello AI"
        val requestBody = ChatStreamRequest(message = userQuery, conversationId = null)
        val conversationUuidForChunks = UUID.randomUUID()

        val chunk1 = ChatResponseChunk(conversationId = conversationUuidForChunks.toString(), content = "AI says:")
        val chunk2 = ChatResponseChunk(conversationId = conversationUuidForChunks.toString(), content = " Hello there!")
        val sseEvent1 = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk1).build()
        val sseEvent2 = ServerSentEvent.builder<ChatResponseChunk>().id("2").event("message").data(chunk2).build()

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, null, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(sseEvent1, sseEvent2)

        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>()
            .consumeWith<WebTestClient.ListBodySpec<ChatResponseChunk>> { result ->
                val chunks = result.responseBody
                assertNotNull(chunks)
                assertEquals(2, chunks.size)
                assertEquals(chunk1.content, chunks[0].content)
                assertEquals(chunk1.conversationId, chunks[0].conversationId)
                assertEquals(chunk2.content, chunks[1].content)
                assertEquals(chunk2.conversationId, chunks[1].conversationId)
            }
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat with message and conversationId should return SSE stream`() {
        val userQuery = "Tell me more"
        val conversationIdParam = UUID.randomUUID().toString()
        val requestBody = ChatStreamRequest(message = userQuery, conversationId = conversationIdParam)

        val chunk1 = ChatResponseChunk(conversationId = conversationIdParam, content = "Okay,")
        val chunk2 = ChatResponseChunk(conversationId = conversationIdParam, content = " what do you want to know?")
        val sseEvent1 = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk1).build()
        val sseEvent2 = ServerSentEvent.builder<ChatResponseChunk>().id("2").event("message").data(chunk2).build()

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationIdParam, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(sseEvent1, sseEvent2)

        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>().hasSize(2)
            .contains(chunk1, chunk2)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat when coordinator returns empty flux should return empty SSE stream`() {
        val userQuery = "Anything new?"
        val conversationIdParam = UUID.randomUUID().toString()
        val requestBody = ChatStreamRequest(message = userQuery, conversationId = conversationIdParam)

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationIdParam, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.empty()

        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>().hasSize(0)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat when coordinator returns an error event should propagate it`() {
        val userQuery = "This might fail"
        val requestBody = ChatStreamRequest(message = userQuery, conversationId = null)
        val conversationUuidForError = UUID.randomUUID()
        val errorMessage = "A critical error occurred in AI"

        val errorSseEvent = createErrorEvent(conversationUuidForError, errorMessage)

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, null, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(errorSseEvent)

        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>()
            .consumeWith<WebTestClient.ListBodySpec<ChatResponseChunk>> { result ->
                val chunks = result.responseBody
                assertNotNull(chunks)
                assertEquals(1, chunks.size)
                assertEquals(conversationUuidForError.toString(), chunks[0].conversationId)
                assertTrue(chunks[0].content.contains(errorMessage))
            }
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat with invalid JSON should return bad request`() {
        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"invalid\": \"json\"}").accept(MediaType.TEXT_EVENT_STREAM).exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat with missing message field should return bad request`() {
        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"conversationId\": \"test-123\"}").accept(MediaType.TEXT_EVENT_STREAM).exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat should pass request parameters correctly to coordinator`() {
        val userQuery = "Test message for parameter verification"
        val conversationId = "test-conversation-123"
        val requestBody = ChatStreamRequest(message = userQuery, conversationId = conversationId)

        val chunk = ChatResponseChunk(conversationId = conversationId, content = "Response")
        val sseEvent = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk).build()

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationId, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(sseEvent)

        webTestClient.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)

        io.mockk.verify {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationId, MOCK_USER_EXTERNAL_ID
            )
        }
    }
}