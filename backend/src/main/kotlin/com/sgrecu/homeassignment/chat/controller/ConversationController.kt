package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.service.ConversationService
import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * REST controller for managing conversations.
 */
@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService
) {
    /**
     * Gets all conversations for the authenticated user.
     */
    @GetMapping
    fun getUserConversations(
        @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<Conversation> {
        val externalId = principal.externalId
        return conversationService.getUserConversations(externalId)
    }

    /**
     * Gets a specific conversation by ID.
     */
    @GetMapping("/{conversationId}")
    fun getConversation(
        @PathVariable conversationId: UUID, @AuthenticationPrincipal principal: UserPrincipal
    ): Mono<Conversation> {
        val externalId = principal.externalId
        return conversationService.getConversation(conversationId, externalId)
    }

    /**
     * Gets all messages for a specific conversation.
     */
    @GetMapping("/{conversationId}/messages")
    fun getConversationMessages(
        @PathVariable conversationId: UUID, @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<Message> {
        val externalId = principal.externalId
        return conversationService.getConversationMessages(conversationId, externalId)
    }

    /**
     * Updates a conversation's title.
     */
    @PutMapping("/{conversationId}")
    fun updateConversation(
        @PathVariable conversationId: UUID,
        @RequestBody request: UpdateConversationRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Mono<Conversation> {
        val externalId = principal.externalId
        return conversationService.updateConversationTitle(conversationId, request.title, externalId)
    }

    /**
     * Deletes a conversation.
     */
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteConversation(
        @PathVariable conversationId: UUID, @AuthenticationPrincipal principal: UserPrincipal
    ): Mono<Void> {
        val externalId = principal.externalId
        return conversationService.deleteConversation(conversationId, externalId)
    }
}

/**
 * Request for updating a conversation.
 */
data class UpdateConversationRequest(
    val title: String
) 