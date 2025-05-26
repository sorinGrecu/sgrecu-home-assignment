package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.service.ChatCoordinator
import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Controller for chat interactions with the AI model.
 * Supports Server-Sent Events (SSE) for streaming chat responses.
 *
 * This controller provides endpoints for real-time chat communication with AI models,
 * enabling streaming responses that appear progressively to users.
 */
@RestController
@RequestMapping("/api/chat")
class ChatController(
    /**
     * Service responsible for coordinating chat interactions,
     * including conversation management, prompt handling, and AI model integration.
     */
    private val chatCoordinator: ChatCoordinator
) {

    /**
     * Endpoint for streaming chat responses.
     * Uses Server-Sent Events (SSE) to stream the AI responses back to the client.
     *
     * @param userQuery The user's message to the AI (must be between 1 and 4000 characters)
     * @param conversationId Optional ID to continue an existing conversation. If null, a new conversation will be created.
     * @param principal The authenticated user principal containing user identity information
     * @return A reactive stream of chat response chunks formatted as Server-Sent Events,
     *         where each chunk contains a portion of the AI's response
     */
    @GetMapping(path = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @RequestParam("message") userQuery: String,
        @RequestParam("conversationId", required = false) conversationId: String?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<ServerSentEvent<ChatResponseChunk>> {
        return chatCoordinator.streamChat(userQuery, conversationId, principal.getUserId())
    }
}
