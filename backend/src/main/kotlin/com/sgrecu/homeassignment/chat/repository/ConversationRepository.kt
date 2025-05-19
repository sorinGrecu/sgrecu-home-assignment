package com.sgrecu.homeassignment.chat.repository

import com.sgrecu.homeassignment.chat.model.Conversation
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.*

/**
 * Reactive repository for Conversation entities.
 */
@Repository
interface ConversationRepository : ReactiveCrudRepository<Conversation, UUID> {

    /**
     * Find all conversations for a user, ordered by last update.
     */
    fun findByUserIdOrderByUpdatedAtDesc(userId: String): Flux<Conversation>
} 