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

        webTestClient.get().uri { uriBuilder ->
            uriBuilder.path("/api/chat/stream").queryParam("message", userQuery).build()
        }.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>()
            .consumeWith<WebTestClient.ListBodySpec<ChatResponseChunk>> { result ->
                val chunks = result.responseBody
                kotlin.test.assertNotNull(chunks)
                kotlin.test.assertEquals(2, chunks.size)
                kotlin.test.assertEquals(chunk1.content, chunks[0].content)
                kotlin.test.assertEquals(chunk1.conversationId, chunks[0].conversationId)
                kotlin.test.assertEquals(chunk2.content, chunks[1].content)
                kotlin.test.assertEquals(chunk2.conversationId, chunks[1].conversationId)
            }
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat with message and conversationId should return SSE stream`() {
        val userQuery = "Tell me more"
        val conversationIdParam = UUID.randomUUID().toString()

        val chunk1 = ChatResponseChunk(conversationId = conversationIdParam, content = "Okay,")
        val chunk2 = ChatResponseChunk(conversationId = conversationIdParam, content = " what do you want to know?")
        val sseEvent1 = ServerSentEvent.builder<ChatResponseChunk>().id("1").event("message").data(chunk1).build()
        val sseEvent2 = ServerSentEvent.builder<ChatResponseChunk>().id("2").event("message").data(chunk2).build()

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationIdParam, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(sseEvent1, sseEvent2)

        webTestClient.get().uri { uriBuilder ->
            uriBuilder.path("/api/chat/stream").queryParam("message", userQuery)
                .queryParam("conversationId", conversationIdParam).build()
        }.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>().hasSize(2)
            .contains(chunk1, chunk2)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat when coordinator returns empty flux should return empty SSE stream`() {
        val userQuery = "Anything new?"
        val conversationIdParam = UUID.randomUUID().toString()

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, conversationIdParam, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.empty()

        webTestClient.get().uri { uriBuilder ->
            uriBuilder.path("/api/chat/stream").queryParam("message", userQuery)
                .queryParam("conversationId", conversationIdParam).build()
        }.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>().hasSize(0)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `streamChat when coordinator returns an error event should propagate it`() {
        val userQuery = "This might fail"
        val conversationUuidForError = UUID.randomUUID()
        val errorMessage = "A critical error occurred in AI"

        val errorSseEvent = createErrorEvent(conversationUuidForError, errorMessage)

        every {
            testMockBeansConfig.chatCoordinatorMock.streamChat(
                userQuery, null, MOCK_USER_EXTERNAL_ID
            )
        } returns Flux.just(errorSseEvent)

        webTestClient.get().uri { uriBuilder ->
            uriBuilder.path("/api/chat/stream").queryParam("message", userQuery).build()
        }.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk.expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).expectBodyList<ChatResponseChunk>()
            .consumeWith<WebTestClient.ListBodySpec<ChatResponseChunk>> { result ->
                val chunks = result.responseBody
                kotlin.test.assertNotNull(chunks)
                kotlin.test.assertEquals(1, chunks.size)
                kotlin.test.assertEquals(conversationUuidForError.toString(), chunks[0].conversationId)
                kotlin.test.assertTrue(chunks[0].content.contains(errorMessage))
            }
    }
}