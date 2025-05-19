package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.service.ConversationService
import com.sgrecu.homeassignment.config.AppProperties
import com.sgrecu.homeassignment.config.TestConfig
import com.sgrecu.homeassignment.security.config.TestSecurityConfig
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.oauth.GoogleAuthProperties
import com.sgrecu.homeassignment.security.util.WithMockUserPrincipal
import com.sgrecu.homeassignment.security.util.WithMockUserPrincipalSecurityContextFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

private const val MOCK_USER_EXTERNAL_ID = "test-user-conversation-id-123"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(
    TestSecurityConfig::class,
    WithMockUserPrincipalSecurityContextFactory::class,
    ConversationControllerIntegrationTest.TestMockBeansConfig::class,
    TestConfig::class
)
class ConversationControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties::class, GoogleAuthProperties::class, AppProperties::class)
    internal class TestMockBeansConfig {
        val conversationServiceMock = mockk<ConversationService>(relaxed = true)

        @Bean
        fun conversationService(): ConversationService {
            return conversationServiceMock
        }

        @Bean
        fun flyway(): Flyway {
            return mockk<Flyway>(relaxed = true)
        }
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var testMockBeansConfig: TestMockBeansConfig

    private val now = Instant.now()
    private val conversationId = UUID.randomUUID()
    private val secondConversationId = UUID.randomUUID()
    private val title = "Test Conversation"
    private val updatedTitle = "Updated Conversation Title"

    private val testConversation = Conversation(
        id = conversationId,
        userId = MOCK_USER_EXTERNAL_ID,
        title = title,
        createdAt = now.minusSeconds(3600),
        updatedAt = now
    )

    private val secondTestConversation = Conversation(
        id = secondConversationId,
        userId = MOCK_USER_EXTERNAL_ID,
        title = "Another Conversation",
        createdAt = now.minusSeconds(7200),
        updatedAt = now.minusSeconds(100)
    )

    private val testMessage = Message(
        id = 1L, conversationId = conversationId, role = MessageRoleEnum.USER, content = "Hello", createdAt = now
    )

    private val testMessage2 = Message(
        id = 2L,
        conversationId = conversationId,
        role = MessageRoleEnum.ASSISTANT,
        content = "How can I help you?",
        createdAt = now.plusSeconds(5)
    )

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `getUserConversations should return list of conversations`() {
        // Given
        val conversations = listOf(testConversation, secondTestConversation)
        every {
            testMockBeansConfig.conversationServiceMock.getUserConversations(MOCK_USER_EXTERNAL_ID)
        } returns Flux.fromIterable(conversations)

        // When/Then
        webTestClient.get().uri("/api/conversations").exchange().expectStatus().isOk.expectBodyList<Conversation>()
            .hasSize(2).contains(testConversation, secondTestConversation)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `getConversation should return specific conversation`() {
        // Given
        every {
            testMockBeansConfig.conversationServiceMock.getConversation(conversationId, MOCK_USER_EXTERNAL_ID)
        } returns Mono.just(testConversation)

        // When/Then
        webTestClient.get().uri("/api/conversations/{conversationId}", conversationId).exchange()
            .expectStatus().isOk.expectBody<Conversation>().isEqualTo(testConversation)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `getConversationMessages should return messages`() {
        // Given
        val messages = listOf(testMessage, testMessage2)
        every {
            testMockBeansConfig.conversationServiceMock.getConversationMessages(conversationId, MOCK_USER_EXTERNAL_ID)
        } returns Flux.fromIterable(messages)

        // When/Then
        webTestClient.get().uri("/api/conversations/{conversationId}/messages", conversationId).exchange()
            .expectStatus().isOk.expectBodyList<Message>().hasSize(2).contains(testMessage, testMessage2)
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `updateConversation should update conversation title`() {
        // Given
        val updatedConversation = testConversation.copy(title = updatedTitle)
        every {
            testMockBeansConfig.conversationServiceMock.updateConversationTitle(
                conversationId, updatedTitle, MOCK_USER_EXTERNAL_ID
            )
        } returns Mono.just(updatedConversation)

        // When/Then
        webTestClient.put().uri("/api/conversations/{conversationId}", conversationId)
            .contentType(MediaType.APPLICATION_JSON).bodyValue(UpdateConversationRequest(updatedTitle)).exchange()
            .expectStatus().isOk.expectBody<Conversation>().consumeWith { response ->
                val conversation = response.responseBody
                assert(conversation != null)
                assert(conversation!!.id == conversationId)
                assert(conversation.title == updatedTitle)
                assert(conversation.userId == MOCK_USER_EXTERNAL_ID)
            }
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `deleteConversation should delete conversation`() {
        // Given
        every {
            testMockBeansConfig.conversationServiceMock.deleteConversation(conversationId, MOCK_USER_EXTERNAL_ID)
        } returns Mono.empty()

        // When/Then
        webTestClient.delete().uri("/api/conversations/{conversationId}", conversationId).exchange()
            .expectStatus().isNoContent.expectBody().isEmpty

        verify {
            testMockBeansConfig.conversationServiceMock.deleteConversation(conversationId, MOCK_USER_EXTERNAL_ID)
        }
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `getConversation should return 404 when conversation not found`() {
        // Given
        every {
            testMockBeansConfig.conversationServiceMock.getConversation(conversationId, MOCK_USER_EXTERNAL_ID)
        } returns Mono.error(com.sgrecu.homeassignment.chat.exception.ConversationNotFoundException(conversationId.toString()))

        // When/Then
        webTestClient.get().uri("/api/conversations/{conversationId}", conversationId).exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUserPrincipal(externalId = MOCK_USER_EXTERNAL_ID)
    fun `getConversation should return 403 when user is not authorized`() {
        // Given
        every {
            testMockBeansConfig.conversationServiceMock.getConversation(conversationId, MOCK_USER_EXTERNAL_ID)
        } returns Mono.error(
            com.sgrecu.homeassignment.chat.exception.UnauthorizedConversationAccessException(
                conversationId.toString(), MOCK_USER_EXTERNAL_ID
            )
        )

        // When/Then
        webTestClient.get().uri("/api/conversations/{conversationId}", conversationId).exchange()
            .expectStatus().isForbidden
    }
} 