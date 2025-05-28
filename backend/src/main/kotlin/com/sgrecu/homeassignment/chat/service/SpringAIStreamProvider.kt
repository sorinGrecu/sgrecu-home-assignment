package com.sgrecu.homeassignment.chat.service

import mu.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Implementation of AIStreamProvider using Spring AI client.
 */
@Service
class SpringAIStreamProvider(private val chatClient: ChatClient.Builder) : AIStreamProvider {
    private val logger = KotlinLogging.logger {}

    /**
     * Creates a stream of responses from the AI model.
     *
     * @param userQuery The user's query to send to the model
     * @return A flux of response content chunks
     */
    override fun createResponseStream(userQuery: String): Flux<String> =
        chatClient.build().prompt().user(userQuery).stream().content()
            .doOnSubscribe { logger.debug { "AI stream started for query: length=${userQuery.length}" } }
            .doOnComplete { logger.debug { "AI stream completed" } }.share()
}