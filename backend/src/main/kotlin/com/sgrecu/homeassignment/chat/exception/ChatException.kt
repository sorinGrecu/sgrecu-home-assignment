package com.sgrecu.homeassignment.chat.exception

import org.springframework.http.HttpStatus

/**
 * Base class for all chat-related exceptions.
 * Provides a consistent way to handle errors in the chat domain.
 */
sealed class ChatException(
    override val message: String, val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when a conversation cannot be found.
 */
class ConversationNotFoundException(id: String) : ChatException(
    "Conversation not found: $id", HttpStatus.NOT_FOUND
)

/**
 * Exception thrown when a user attempts to access a conversation they don't own.
 */
class UnauthorizedConversationAccessException(conversationId: String, userId: String) : ChatException(
    "User $userId is not authorized to access conversation $conversationId", HttpStatus.FORBIDDEN
)

/**
 * Exception thrown when an AI model fails to generate a response.
 */
class AIResponseFailedException(message: String = "Failed to generate AI response", cause: Throwable? = null) :
    ChatException(message, HttpStatus.SERVICE_UNAVAILABLE, cause)
