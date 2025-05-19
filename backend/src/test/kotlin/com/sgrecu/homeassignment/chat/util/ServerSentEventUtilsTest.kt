package com.sgrecu.homeassignment.chat.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.*

@DisplayName("ServerSentEventUtils Tests")
class ServerSentEventUtilsTest {

    private val conversationId = UUID.randomUUID()

    @Test
    fun `mapToServerEvents transforms content to SSE events`() {
        // Given
        val contentFlux = Flux.just("Hello", ", ", "world", "!")

        // When
        val result = mapToServerEvents(contentFlux, conversationId)

        // Then
        StepVerifier.create(result).expectNextMatches { event ->
                event.event() == "message" && event.id() == "1" && event.data()?.conversationId == conversationId.toString() && event.data()?.content == "Hello"
            }.expectNextMatches { event ->
                event.event() == "message" && event.id() == "2" && event.data()?.content == ", "
            }.expectNextMatches { event ->
                event.event() == "message" && event.id() == "3" && event.data()?.content == "world"
            }.expectNextMatches { event ->
                event.event() == "message" && event.id() == "4" && event.data()?.content == "!"
            }.verifyComplete()
    }

    @Test
    fun `mapToServerEvents creates error event when stream errors`() {
        // Given
        val errorMessage = "Test error message"
        val contentFlux = Flux.concat(
            Flux.just("Normal content"), Flux.error(RuntimeException(errorMessage))
        )

        // When
        val result = mapToServerEvents(contentFlux, conversationId)

        // Then
        StepVerifier.create(result).expectNextMatches { event ->
                event.event() == "message" && event.id() == "1" && event.data()?.conversationId == conversationId.toString() && event.data()?.content == "Normal content"
            }.expectNextMatches { event ->
                event.event() == "error" && event.id() == "error" && event.data()?.conversationId == conversationId.toString() && event.data()?.content?.contains(
                    errorMessage
                ) == true
            }.verifyComplete()
    }

    @Test
    fun `mapToServerEvents handles null error message gracefully`() {
        // Given
        val contentFlux = Flux.concat(
            Flux.just("Content"), Flux.error(RuntimeException()) // No error message
        )

        // When
        val result = mapToServerEvents(contentFlux, conversationId)

        // Then
        StepVerifier.create(result).expectNextCount(1).expectNextMatches { event ->
                event.event() == "error" && event.id() == "error" && event.data()?.content?.contains("Unknown error") == true
            }.verifyComplete()
    }

    @Test
    fun `createErrorEvent returns properly formatted SSE error event`() {
        // Given
        val errorMessage = "Test error message"

        // When
        val errorEvent = createErrorEvent(conversationId, errorMessage)

        // Then
        assert(errorEvent.event() == "error")
        assert(errorEvent.id() == "error")
        assert(errorEvent.data()?.conversationId == conversationId.toString())
        assert(errorEvent.data()?.content?.contains(errorMessage) == true)
    }
} 