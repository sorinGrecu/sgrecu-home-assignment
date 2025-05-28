package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.chat.model.ChatResponseChunk
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.util.createErrorEvent
import com.sgrecu.homeassignment.chat.util.mapToServerEvents
import com.sgrecu.homeassignment.config.AppProperties
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
    private val messagePersister: MessagePersister,
    private val appProperties: AppProperties
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Streams a chat interaction as Server-Sent Events.
     *
     * Validates the user query for length and content before processing.
     * Creates or continues a conversation and streams AI responses as SSE events.
     *
     * @param userQuery The user's input text (must not be blank and within configured length limits)
     * @param conversationId Optional UUID to continue an existing conversation
     * @param userId The user's ID for access control
     * @return A Flux of SSE events containing chat response chunks
     * @throws IllegalArgumentException if userQuery is blank or exceeds maximum length
     */
    @Transactional
    fun streamChat(
        userQuery: String, conversationId: String?, userId: String
    ): Flux<ServerSentEvent<ChatResponseChunk>> = Flux.defer {
        validateMessage(userQuery)
        val convId = parseUuid(conversationId)
        val title = if (userQuery.length > 30) "${userQuery.take(30)}..." else userQuery
        findOrCreateConversation(convId, userId, title, userQuery).flatMapMany { id ->
            processAIResponse(id, userQuery)
        }
    }.doOnError { logger.error("Chat stream error: ${it.message}") }

    /**
     * Finds or creates a conversation and saves the user message.
     */
    private fun findOrCreateConversation(
        conversationId: UUID?, userId: String, title: String, userQuery: String
    ): Mono<UUID> =
        conversationService.findOrCreateConversation(conversationId, userId, title).flatMap { conversation ->
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


    /**
     * Processes the AI response, coordinating between the services.
     */
    private fun processAIResponse(
        conversationId: UUID, userQuery: String
    ): Flux<ServerSentEvent<ChatResponseChunk>> = Flux.defer {
        val aiResponseStream = aiTransport.createFilteredResponseStream(userQuery, conversationId)
        val sharedResponseStream = aiResponseStream.publish().refCount(1)
        val clientStream = mapToServerEvents(sharedResponseStream, conversationId)
        val persistenceStream =
            messagePersister.saveMessage(sharedResponseStream, conversationId, MessageRoleEnum.ASSISTANT)
                .doOnError { error ->
                    logger.error { "Message persistence failed: ${error.message}" }
                }.onErrorComplete().thenMany(Flux.empty<ServerSentEvent<ChatResponseChunk>>())

        Flux.merge(clientStream, persistenceStream)
    }.onErrorResume { ex ->
        when (ex) {
            is AIResponseFailedException -> {
                logger.error { "AI response failed: ${ex.message}" }
                Flux.just(createErrorEvent(conversationId, ex.message))
            }

            else -> {
                logger.error { "AI response processing failed: ${ex.message}" }
                val errorMessage = "Failed to process AI response: ${ex.message ?: "Unknown error"}"
                val errorEvent = createErrorEvent(conversationId, errorMessage)
                Flux.just(errorEvent).concatWith(Flux.error(ex))
            }
        }
    }


    /**
     * Validates the user message for length.
     */
    private fun validateMessage(userQuery: String) {
        if (userQuery.isBlank()) {
            throw IllegalArgumentException("Message cannot be empty")
        }
        if (userQuery.length > appProperties.chat.maxMessageLength) {
            throw IllegalArgumentException("Message length exceeds maximum allowed length of ${appProperties.chat.maxMessageLength} characters")
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