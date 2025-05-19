package com.sgrecu.homeassignment.chat.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Model class representing a chunk of a chat response for streaming.
 * This is the data format sent to the client via Server-Sent Events (SSE).
 * Each chunk contains a portion of the AI's response and the conversation UUID.
 */
data class ChatResponseChunk(
    /**
     * The UUID of the conversation this chunk belongs to.
     * This allows the client to associate chunks with specific conversations.
     */
    @JsonProperty("conversationId") val conversationId: String,

    /**
     * The actual content of this response chunk.
     * For most chunks, this will be a token or word from the AI response.
     * For error events, this contains error information.
     */
    @JsonProperty("content") val content: String
) : Serializable 