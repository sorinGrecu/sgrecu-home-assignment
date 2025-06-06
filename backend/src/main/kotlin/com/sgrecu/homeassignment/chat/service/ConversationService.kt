package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.exception.ConversationNotFoundException
import com.sgrecu.homeassignment.chat.exception.UnauthorizedConversationAccessException
import com.sgrecu.homeassignment.chat.model.Conversation
import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.repository.ConversationRepository
import com.sgrecu.homeassignment.chat.repository.MessageRepository
import mu.KotlinLogging
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.util.*

/**
 * Service for managing conversations and messages.
 */
@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val clock: Clock
) {
    private val logger = KotlinLogging.logger {}

    private fun now() = Instant.now(clock)

    /**
     * Creates a new conversation for a user.
     *
     * @param userId The ID of the user who owns the conversation
     * @param title Optional title for the conversation
     * @return A Mono emitting the created conversation
     */
    fun createConversation(userId: String, title: String): Mono<Conversation> {
        val now = now()
        return conversationRepository.save(
            Conversation(
                userId = userId, title = title, createdAt = now, updatedAt = now()
            )
        )
    }

    /**
     * Gets all conversations for a user, sorted by most recent update.
     *
     * @param userId The ID of the user
     * @return A Flux emitting the user's conversations
     */
    fun getUserConversations(userId: String): Flux<Conversation> =
        conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)

    /**
     * Finds a conversation by ID and verifies the user has access to it.
     * Throws an exception if the conversation doesn't exist or the user is not authorized.
     *
     * @param conversationId The ID of the conversation to find
     * @param userId The ID of the user requesting access
     * @return A Mono emitting the conversation if found and access is authorized
     * @throws ConversationNotFoundException if the conversation doesn't exist
     * @throws UnauthorizedConversationAccessException if the user is not the owner
     */
    private fun findConversationWithAccess(conversationId: UUID, userId: String): Mono<Conversation> =
        conversationRepository.findById(conversationId).switchIfEmpty(Mono.defer {
            Mono.error(ConversationNotFoundException(conversationId.toString()))
        }).filter { it.userId == userId }.switchIfEmpty(Mono.defer {
            Mono.error(UnauthorizedConversationAccessException(conversationId.toString(), userId))
        })

    /**
     * Gets a conversation by ID, verifying the user has access.
     *
     * @param conversationId The ID of the conversation to retrieve
     * @param userId The ID of the user requesting access
     * @return A Mono emitting the conversation if found and user is authorized
     * @throws ConversationNotFoundException if the conversation doesn't exist
     * @throws UnauthorizedConversationAccessException if the user is not the owner
     */
    fun getConversation(conversationId: UUID, userId: String): Mono<Conversation> {
        return findConversationWithAccess(conversationId, userId)
    }

    /**
     * Updates a conversation's title.
     *
     * @param conversationId The ID of the conversation to update
     * @param title The new title
     * @param userId The ID of the user requesting the update
     * @return A Mono emitting the updated conversation
     * @throws ConversationNotFoundException if the conversation doesn't exist
     * @throws UnauthorizedConversationAccessException if the user is not the owner
     */
    fun updateConversationTitle(conversationId: UUID, title: String, userId: String): Mono<Conversation> {
        return findConversationWithAccess(conversationId, userId).flatMap { conversation ->
            val updated = conversation.copy(
                title = title, updatedAt = now()
            )
            conversationRepository.save(updated)
        }
    }

    @Transactional
    internal fun addMessageInternal(
        conversationId: UUID, role: MessageRoleEnum, content: String
    ): Mono<Message> {
        val now = now()
        val message = Message(
            conversationId = conversationId, role = role, content = content, createdAt = now
        )

        return conversationRepository.findById(conversationId).map { it.copy(updatedAt = now) }
            .flatMap(conversationRepository::save).onErrorResume { handleConversationUpdateError(it, conversationId) }
            .then(messageRepository.save(message)).onErrorResume { handleMessageSaveError(it, conversationId) }
    }

    /**
     * Handles database exceptions during conversation updates.
     */
    private fun handleConversationUpdateError(error: Throwable, conversationId: UUID): Mono<Conversation> {
        return if (error is TransientDataAccessResourceException && error.message?.contains("Row with Id [$conversationId] does not exist") == true) {
            logger.warn { "Update attempted on deleted conversation: id=$conversationId" }
            Mono.empty()
        } else {
            Mono.error(error)
        }
    }

    /**
     * Handles database exceptions during message saves.
     */
    private fun handleMessageSaveError(error: Throwable, conversationId: UUID): Mono<Message> {
        logger.error(error) { "Failed to save message for conversation: conversationId=$conversationId" }
        return Mono.error(error)
    }

    /**
     * Gets all messages for a conversation in chronological order.
     * Note: This method does not verify user access and should only be used internally
     * or after access verification.
     *
     * @param conversationId The ID of the conversation
     * @return A Flux emitting the conversation's messages
     */
    internal fun findMessagesByConversationId(conversationId: UUID): Flux<Message> =
        messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)


    /**
     * Gets all messages for a conversation in chronological order, verifying the user has access.
     *
     * @param conversationId The ID of the conversation
     * @param userId The ID of the user requesting the messages
     * @return A Flux emitting the conversation's messages
     * @throws ConversationNotFoundException if the conversation doesn't exist
     * @throws UnauthorizedConversationAccessException if the user is not the owner
     */
    fun getConversationMessages(conversationId: UUID, userId: String): Flux<Message> =
        findConversationWithAccess(conversationId, userId).flatMapMany { findMessagesByConversationId(conversationId) }

    /**
     * Deletes a conversation and all its messages.
     * Note: This method does not verify user access and should only be used internally
     * or after access verification.
     *
     * @param conversationId The ID of the conversation to delete
     * @return A Mono that completes when the operation is done
     */
    @Transactional
    internal fun deleteConversationInternal(conversationId: UUID): Mono<Void> =
        messageRepository.deleteAllByConversationId(conversationId)
            .then(conversationRepository.deleteById(conversationId))

    /**
     * Deletes a conversation and all its messages, verifying the user has access.
     *
     * @param conversationId The ID of the conversation to delete
     * @param userId The ID of the user requesting deletion
     * @return A Mono that completes when the operation is done
     * @throws ConversationNotFoundException if the conversation doesn't exist
     * @throws UnauthorizedConversationAccessException if the user is not the owner
     */
    @Transactional
    fun deleteConversation(conversationId: UUID, userId: String): Mono<Void> =
        findConversationWithAccess(conversationId, userId).flatMap { deleteConversationInternal(conversationId) }


    /**
     * Finds an existing conversation by ID or creates a new one if not found.
     *
     * @param conversationId The UUID of the conversation to find, may be null
     * @param userId The user ID who owns or will own the conversation
     * @param title Optional title for a new conversation
     * @return A Mono emitting the existing or newly created conversation
     */
    @Transactional
    fun findOrCreateConversation(conversationId: UUID?, userId: String, title: String): Mono<Conversation> =
        if (conversationId == null) {
            logger.debug { "Creating new conversation: user=$userId" }
            createConversation(userId, title)
        } else {
            conversationRepository.findById(conversationId)
                .doOnNext { logger.debug { "Found conversation: id=$conversationId, user=$userId" } }
                .filter { conversation ->
                    val authorised = conversation.userId == userId
                    if (!authorised) logger.warn {
                        "Unauthorized access attempt: conversation=$conversationId, owner=${conversation.userId}, requester=$userId"
                    }
                    authorised
                }.switchIfEmpty(
                    Mono.defer {
                        logger.debug { "Conversation not found or unauthorized, creating new: id=$conversationId" }
                        createConversation(userId, title)
                    })
        }
}