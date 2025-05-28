package com.sgrecu.homeassignment.chat.util

import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import mu.KotlinLogging
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Transforms a Flux of filtered content into SSE events.
 *
 * @param contentFlux The filtered content flux
 * @param conversationId The conversation ID
 * @return A Flux of SSE events containing chat response chunks
 */
fun mapToServerEvents(
    contentFlux: Flux<String>, conversationId: UUID
): Flux<ServerSentEvent<ChatResponseChunk>> = contentFlux.index().map { t ->
    ServerSentEvent.builder<ChatResponseChunk>().id((t.t1 + 1).toString()).event("message")
        .data(ChatResponseChunk(conversationId.toString(), t.t2)).build()
}.onErrorResume { e ->
    logger.error("SSE streaming error: conversation={}, error={}", conversationId, e.message, e)
    Flux.just(createErrorEvent(conversationId, e.message ?: "Unknown error"))
}

/**
 * Creates an error event for SSE.
 *
 * @param conversationId The conversation ID
 * @param errorMessage The error message
 * @return A ServerSentEvent containing the error information
 */
fun createErrorEvent(conversationId: UUID, errorMessage: String): ServerSentEvent<ChatResponseChunk> {
    return ServerSentEvent.builder<ChatResponseChunk>().id("error").event("error").data(
        ChatResponseChunk(
            conversationId = conversationId.toString(), content = "An error occurred during streaming: $errorMessage"
        )
    ).build()
} 