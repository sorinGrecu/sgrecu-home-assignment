package com.sgrecu.homeassignment.chat.strategy

import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * Interface defining a strategy for saving message content streams.
 *
 * Available strategies:
 * - EndOfStreamMessageSaveStrategy: Saves the complete message only after the stream has ended.
 * Possible strategy implementations:
 * - BufferedMessageSaveStrategy: Saves content chunks in batches during streaming.
 *
 * This interface allows for easy extension with additional saving strategies as requirements evolve.
 * New strategies can be implemented by following the Strategy pattern.
 */
interface MessageSaveStrategy {
    /**
     * Saves a stream of content as messages with the associated conversation ID and role.
     *
     * @param contents The stream of content to save
     * @param conversationId The conversation ID to associate with the messages
     * @param role The role of the message
     * @return A Mono that completes when the save operation is done
     */
    fun save(contents: Flux<String>, conversationId: UUID, role: MessageRoleEnum): Mono<Void>

    /**
     * Returns the unique name of this save strategy.
     * This name is used for strategy selection via configuration.
     *
     * @return The strategy name
     */
    fun getName(): String
} 