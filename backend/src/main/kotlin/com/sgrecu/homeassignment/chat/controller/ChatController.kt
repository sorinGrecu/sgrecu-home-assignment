package com.sgrecu.homeassignment.chat.controller

import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.service.ChatCoordinator
import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Request body for chat streaming endpoint
 */
data class ChatStreamRequest(
    val message: String, val conversationId: String? = null
)

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
     * Endpoint for streaming chat responses via POST request.
     * Uses Server-Sent Events (SSE) to stream the AI responses back to the client.
     *
     * POST is more appropriate for chat messages as:
     * - Message content can be large and complex
     * - Avoids URL length limitations
     * - Better security (no sensitive data in URL/logs)
     * - Supports structured request bodies
     *
     * @param request The chat request containing message and optional conversation ID
     * @param principal The authenticated user principal containing user identity information
     * @return A reactive stream of chat response chunks formatted as Server-Sent Events,
     *         where each chunk contains a portion of the AI's response
     */
    @PostMapping(path = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @RequestBody request: ChatStreamRequest, @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<ServerSentEvent<ChatResponseChunk>> {
        return chatCoordinator.streamChat(request.message, request.conversationId, principal.getUserId())
    }
}
