package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.monitoring.AIInfoEndpoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

@DisplayName("AITransport Tests")
class AITransportTest {

    private lateinit var aiStreamProvider: AIStreamProvider
    private lateinit var contentFilter: ContentFilter
    private lateinit var aiInfoEndpoint: AIInfoEndpoint
    private lateinit var aiTransport: AITransport
    private val conversationId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        aiStreamProvider = mockk()
        contentFilter = mockk()
        aiInfoEndpoint = mockk(relaxUnitFun = true)
        aiTransport = AITransport(aiStreamProvider, contentFilter, aiInfoEndpoint)
    }

    @Test
    fun `createFilteredResponseStream should filter content and track metrics`() {
        // Given
        val query = "What is AI?"
        val rawResponse = Flux.just("AI", " stands", " for", " Artificial", " Intelligence")
        val filteredResponse = Flux.just("AI stands for Artificial Intelligence")

        every { aiStreamProvider.createResponseStream(query) } returns rawResponse
        every { contentFilter.filterContent(rawResponse) } returns filteredResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId))
            .expectNext("AI stands for Artificial Intelligence").verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream should handle errors from createResponseStream`() {
        // Given
        val query = "What is AI?"
        val exception = RuntimeException("Connection error")

        every { aiStreamProvider.createResponseStream(query) } throws exception

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId))
            .expectError(AIResponseFailedException::class.java).verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
        verify(exactly = 0) { contentFilter.filterContent(any()) }
    }

    @Test
    fun `createFilteredResponseStream should handle errors from content filter`() {
        // Given
        val query = "What is AI?"
        val rawResponse = Flux.just("AI", " stands", " for", " Artificial", " Intelligence")
        val exception = RuntimeException("Filter error")

        every { aiStreamProvider.createResponseStream(query) } returns rawResponse
        every { contentFilter.filterContent(rawResponse) } returns Flux.error(exception)

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId))
            .expectError(RuntimeException::class.java).verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream should always decrement counters even on errors`() {
        // Given
        val query = "What is AI?"
        val exception = RuntimeException("Error")

        every { aiStreamProvider.createResponseStream(query) } throws exception

        // When
        try {
            aiTransport.createFilteredResponseStream(query, conversationId).collectList().block()
        } catch (e: Exception) {
            // Expected exception, ignore
        }

        // Then
        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles empty response stream`() {
        // Given
        val query = "What is AI?"
        val emptyResponse = Flux.empty<String>()

        every { aiStreamProvider.createResponseStream(query) } returns emptyResponse
        every { contentFilter.filterContent(emptyResponse) } returns emptyResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles large response streams`() {
        // Given
        val query = "Write a long essay"
        val largeResponse = Flux.fromIterable((1..1000).map { "token$it" })
        val filteredResponse = Flux.fromIterable((1..500).map { "filtered$it" })

        every { aiStreamProvider.createResponseStream(query) } returns largeResponse
        every { contentFilter.filterContent(largeResponse) } returns filteredResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId).take(10)).expectNext(
            "filtered1",
            "filtered2",
            "filtered3",
            "filtered4",
            "filtered5",
            "filtered6",
            "filtered7",
            "filtered8",
            "filtered9",
            "filtered10"
        ).verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles error with specific message in exception`() {
        // Given
        val query = "What is AI?"
        val customErrorMessage = "Custom API error message"
        val exception = IllegalStateException(customErrorMessage)

        every { aiStreamProvider.createResponseStream(query) } throws exception

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).expectErrorMatches {
            it is AIResponseFailedException && it.message.contains(customErrorMessage)
        }.verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream propagates filter errors with correct type`() {
        // Given
        val query = "What is AI?"
        val rawResponse = Flux.just("AI response")
        val exception = IllegalArgumentException("Invalid token format")

        every { aiStreamProvider.createResponseStream(query) } returns rawResponse
        every { contentFilter.filterContent(rawResponse) } returns Flux.error(exception)

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId))
            .expectError(IllegalArgumentException::class.java).verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles cancelled subscription`() {
        // Given
        val query = "What is AI?"
        val slowResponse = Flux.interval(Duration.ofMillis(100)).map { "token$it" }.take(10)

        every { aiStreamProvider.createResponseStream(query) } returns slowResponse
        every { contentFilter.filterContent(slowResponse) } returns slowResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).expectNext("token0")
            .expectNext("token1").thenCancel().verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles empty query`() {
        // Given
        val query = ""
        val response = Flux.just("Response to empty query")

        every { aiStreamProvider.createResponseStream(query) } returns response
        every { contentFilter.filterContent(response) } returns response

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId))
            .expectNext("Response to empty query").verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles errors with null message`() {
        // Given
        val query = "What is AI?"
        val exception = NullPointerException() // No message

        every { aiStreamProvider.createResponseStream(query) } throws exception

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).expectErrorMatches {
            it is AIResponseFailedException && it.message.contains("Unknown error")
        }.verify()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream continues working after errors`() {
        // Given
        val failingQuery = "fail"
        val successQuery = "succeed"
        val exception = RuntimeException("First request fails")
        val response = Flux.just("Success response")

        every { aiStreamProvider.createResponseStream(failingQuery) } throws exception
        every { aiStreamProvider.createResponseStream(successQuery) } returns response
        every { contentFilter.filterContent(response) } returns response

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(failingQuery, conversationId))
            .expectError(AIResponseFailedException::class.java).verify()

        StepVerifier.create(aiTransport.createFilteredResponseStream(successQuery, conversationId))
            .expectNext("Success response").verifyComplete()

        verify(exactly = 2) { aiInfoEndpoint.incrementActiveRequests() }
        verify(exactly = 2) { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles element count differences when contentFilter filters content`() {
        // Given
        val query = "What is AI?"
        val rawResponse = Flux.just("AI", " ", "", " stands", " for", " ", "Artificial", " ", "Intelligence")
        val filteredResponse = Flux.just("AI", "stands", "for", "Artificial", "Intelligence")

        every { aiStreamProvider.createResponseStream(query) } returns rawResponse
        every { contentFilter.filterContent(rawResponse) } returns filteredResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).expectNext("AI")
            .expectNext("stands").expectNext("for").expectNext("Artificial").expectNext("Intelligence").verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `createFilteredResponseStream handles element count differences when contentFilter aggregates tokens`() {
        // Given
        val query = "What is AI?"
        val rawResponse = Flux.just("AI", " stands", " for", " Artificial", " Intelligence")
        val filteredResponse = Flux.just("AI stands for", " Artificial Intelligence")

        every { aiStreamProvider.createResponseStream(query) } returns rawResponse
        every { contentFilter.filterContent(rawResponse) } returns filteredResponse

        // When/Then
        StepVerifier.create(aiTransport.createFilteredResponseStream(query, conversationId)).expectNext("AI stands for")
            .expectNext(" Artificial Intelligence").verifyComplete()

        verify { aiInfoEndpoint.incrementActiveRequests() }
        verify { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `aiInfoEndpoint correctly tracks concurrent requests on real schedulers`() {
        // Given
        val query1 = "Query 1"
        val query2 = "Query 2"
        val query3 = "Query 3"
        val response1 = Flux.just("Response 1").delayElements(Duration.ofMillis(100))
        val response2 = Flux.just("Response 2").delayElements(Duration.ofMillis(150))
        val response3 = Flux.just("Response 3").delayElements(Duration.ofMillis(50))

        every { aiStreamProvider.createResponseStream(query1) } returns response1
        every { aiStreamProvider.createResponseStream(query2) } returns response2
        every { aiStreamProvider.createResponseStream(query3) } returns response3

        every { contentFilter.filterContent(response1) } returns response1
        every { contentFilter.filterContent(response2) } returns response2
        every { contentFilter.filterContent(response3) } returns response3

        var activeRequestsMax = 0

        every { aiInfoEndpoint.incrementActiveRequests() } answers {
            activeRequestsMax++
            Unit
        }

        every { aiInfoEndpoint.decrementActiveRequests() } answers {
            activeRequestsMax--
            Unit
        }

        // When
        val results = Flux.merge(
            aiTransport.createFilteredResponseStream(query1, UUID.randomUUID())
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel()),
            aiTransport.createFilteredResponseStream(query2, UUID.randomUUID())
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel()),
            aiTransport.createFilteredResponseStream(query3, UUID.randomUUID())
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
        ).collectList().block()

        // Then
        assert(results?.size == 3)
        assert(results?.contains("Response 1") == true)
        assert(results?.contains("Response 2") == true)
        assert(results?.contains("Response 3") == true)

        verify(exactly = 3) { aiInfoEndpoint.incrementActiveRequests() }
        verify(exactly = 3) { aiInfoEndpoint.decrementActiveRequests() }
    }

    @Test
    fun `slice test with real components instead of full mocking`() {
        // Given
        val thinkingConfig = mockk<com.sgrecu.homeassignment.chat.config.ThinkingConfig>()
        every { thinkingConfig.enabled } returns true
        every { thinkingConfig.startTag } returns "<think>"
        every { thinkingConfig.endTag } returns "</think>"

        val aiConfig = mockk<com.sgrecu.homeassignment.chat.config.AIConfig>()
        every { aiConfig.thinking } returns thinkingConfig

        val realContentFilter = ContentFilter(aiConfig)

        val mockAiStreamProvider = mockk<AIStreamProvider>()
        val mockAiInfoEndpoint = mockk<AIInfoEndpoint>(relaxUnitFun = true)

        val transportWithRealFilter = AITransport(mockAiStreamProvider, realContentFilter, mockAiInfoEndpoint)

        val query = "What is AI?"
        val rawResponse = Flux.just(
            "<think>",
            "Let me think about this...",
            "</think>",
            "AI",
            " ",
            "stands",
            " ",
            "for",
            " ",
            "Artificial",
            " ",
            "Intelligence"
        )

        every { mockAiStreamProvider.createResponseStream(query) } returns rawResponse

        // When/Then
        StepVerifier.create(transportWithRealFilter.createFilteredResponseStream(query, conversationId))
            .expectNext("AI").expectNext("stands").expectNext("for").expectNext("Artificial").expectNext("Intelligence")
            .verifyComplete()

        verify { mockAiInfoEndpoint.incrementActiveRequests() }
        verify { mockAiInfoEndpoint.decrementActiveRequests() }
    }
} 