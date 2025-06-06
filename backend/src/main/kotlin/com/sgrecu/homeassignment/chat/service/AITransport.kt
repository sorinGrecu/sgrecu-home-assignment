package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.monitoring.AIInfoEndpoint
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.*

/**
 * Component responsible for interacting with AI models.
 * Handles creating response streams and error handling for AI interactions.
 */
@Component
class AITransport(
    private val aiStreamProvider: AIStreamProvider,
    private val contentFilter: ContentFilter,
    private val aiInfoEndpoint: AIInfoEndpoint
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Creates a filtered stream of AI responses based on the user query.
     *
     * @param userQuery The user's input text
     * @param conversationId The ID of the conversation for logging purposes
     * @return A Flux of filtered content strings from the AI model
     * @throws AIResponseFailedException if the AI service connection fails
     */
    fun createFilteredResponseStream(
        userQuery: String, conversationId: UUID
    ): Flux<String> = Flux.defer {
        aiStreamProvider.createResponseStream(userQuery).transform(contentFilter::filterContent)
    }.onErrorMap { ex ->
        logger.error("AI content filtering error: conversation={}, error={}", conversationId, ex.message)
        when (ex) {
            is IllegalArgumentException -> ex
            else -> {
                logger.error("AI response setup failed: {}", ex.message)
                AIResponseFailedException("Failed to connect to AI service: ${ex.message ?: "Unknown error"}", ex)
            }
        }
    }.subscribeOn(Schedulers.parallel()).doOnSubscribe { aiInfoEndpoint.incrementActiveRequests() }
        .doFinally { aiInfoEndpoint.decrementActiveRequests() }

}