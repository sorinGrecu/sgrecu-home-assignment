package com.sgrecu.homeassignment.chat.repository

import com.sgrecu.homeassignment.chat.model.Message
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * Reactive repository for Message entities.
 */
@Repository
interface MessageRepository : ReactiveCrudRepository<Message, Long> {

    /**
     * Find all messages for a conversation, ordered by creation time.
     */
    fun findByConversationIdOrderByCreatedAtAsc(conversationId: UUID): Flux<Message>

    /**
     * Delete all messages for a conversation in a single operation.
     */
    @Modifying
    @Query("DELETE FROM messages WHERE conversation_id = :id")
    fun deleteAllByConversationId(id: UUID): Mono<Void>
} 