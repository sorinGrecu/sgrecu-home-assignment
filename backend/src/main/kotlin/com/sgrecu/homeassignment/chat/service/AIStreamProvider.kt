package com.sgrecu.homeassignment.chat.service

import reactor.core.publisher.Flux

/**
 * Interface defining a provider for AI response streams.
 * This abstraction enables better testability by allowing mock implementations.
 */
interface AIStreamProvider {
    /**
     * Creates a stream of AI responses for a given prompt.
     *
     * @param userQuery The user's input query/prompt
     * @return A flux of string chunks from the AI model's response
     */
    fun createResponseStream(userQuery: String): Flux<String>
} 