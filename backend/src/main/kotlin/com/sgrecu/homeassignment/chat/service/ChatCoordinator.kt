package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.util.createErrorEvent
import com.sgrecu.homeassignment.chat.util.mapToServerEvents
import mu.KotlinLogging
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * Coordinates the chat interaction workflow between the various specialized components.
 * Acts as an orchestrator for conversation management, AI interaction, persistence, and event mapping.
 */
@Service
class ChatCoordinator(
    private val aiTransport: AITransport,
    private val conversationService: ConversationService,
    private val messagePersister: MessagePersister
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Streams a chat interaction as Server-Sent Events.
     *
     * @param userQuery The user's input text
     * @param conversationId Optional UUID to continue an existing conversation
     * @param userId The user's ID for access control
     * @return A Flux of SSE events containing chat response chunks
     */
    @Transactional
    fun streamChat(
        userQuery: String, conversationId: String?, userId: String
    ): Flux<ServerSentEvent<ChatResponseChunk>> {
        val parsedConversationId = parseUuid(conversationId)
        logger.debug {
            "Chat request: conversationId=$parsedConversationId, userId='$userId', query length=${userQuery.length}"
        }

        val title = userQuery.take(30).let { if (userQuery.length > 30) "$it..." else it }

        return findOrCreateConversation(
            parsedConversationId, userId, title, userQuery
        ).flatMapMany { convId -> processAIResponse(convId, userQuery) }.doOnError { error ->
            logger.error { "Chat stream error: ${error.message}" }
        }
    }

    /**
     * Finds or creates a conversation and saves the user message.
     */
    private fun findOrCreateConversation(
        conversationId: UUID?, userId: String, title: String, userQuery: String
    ): Mono<UUID> {
        return conversationService.findOrCreateConversation(conversationId, userId, title).flatMap { conversation ->
            val id = conversation.id ?: return@flatMap Mono.error(
                IllegalStateException("Conversation has no ID")
            )

            conversationService.addMessageInternal(id, MessageRoleEnum.USER, userQuery).thenReturn(id)
        }.onErrorMap { error ->
            when (error) {
                is IllegalArgumentException, is IllegalStateException -> error
                else -> IllegalStateException("Conversation preparation failed: ${error.message}", error)
            }
        }
    }

    /**
     * Processes the AI response, coordinating between the services.
     */
    private fun processAIResponse(
        conversationId: UUID, userQuery: String
    ): Flux<ServerSentEvent<ChatResponseChunk>> {
        try {
            val aiResponseStream = aiTransport.createFilteredResponseStream(userQuery, conversationId)
            val sharedResponseStream = aiResponseStream.publish().refCount(1)
            val clientStream = mapToServerEvents(sharedResponseStream, conversationId)

            val persistenceStream =
                messagePersister.saveMessage(sharedResponseStream, conversationId, MessageRoleEnum.ASSISTANT)
                    .doOnError { error ->
                        logger.error { "Message persistence failed: ${error.message}" }
                    }.onErrorComplete().thenMany(Flux.empty<ServerSentEvent<ChatResponseChunk>>())

            return Flux.merge(clientStream, persistenceStream)
        } catch (ex: Exception) {
            logger.error { "AI response processing failed: ${ex.message}" }
            val errorMessage = "Failed to process AI response: ${ex.message ?: "Unknown error"}"
            val errorEvent = createErrorEvent(conversationId, errorMessage)

            if (ex is AIResponseFailedException) {
                return Flux.just(errorEvent)
            }

            return Flux.just(errorEvent).concatWith(Flux.error(ex))
        }
    }

    /**
     * Parses a UUID string to a UUID object.
     */
    private fun parseUuid(uuid: String?): UUID? {
        if (uuid.isNullOrBlank()) return null

        return try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid UUID format: $uuid" }
            null
        }
    }
} 