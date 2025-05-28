package com.sgrecu.homeassignment.chat.strategy

import com.sgrecu.homeassignment.chat.model.Message
import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.repository.MessageRepository
import com.sgrecu.homeassignment.chat.service.ContentFilter
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * Strategy that collects all message content and saves it once the stream completes.
 * This is the default strategy used when no specific strategy is configured.
 *
 */
@Primary
@Component
class EndOfStreamMessageSaveStrategy(
    private val messageRepository: MessageRepository, private val contentFilter: ContentFilter
) : MessageSaveStrategy {
    private val logger = KotlinLogging.logger {}

    /**
     * Saves message content after collecting all stream data.
     * Uses a streaming approach to reduce memory usage for large responses.
     */
    override fun save(
        contents: Flux<String>, conversationId: UUID, role: MessageRoleEnum
    ): Mono<Void> {
        logger.debug { "Starting collection for conversation $conversationId" }

        return contentFilter.filterContent(contents).collect({ StringBuilder() }, StringBuilder::append)
            .map(StringBuilder::toString).flatMap { body ->
                if (body.isBlank()) {
                    logger.debug { "No content after filtering: conversation=$conversationId" }
                    Mono.empty()
                } else {
                    logger.debug { "Saving message: conversation=$conversationId, length=${body.length}" }
                    messageRepository.save(Message(conversationId = conversationId, role = role, content = body))
                        .doOnSuccess { logger.debug { "Message saved: id=${it.id}, conversation=$conversationId" } }
                        .then()
                }
            }.doOnError { e -> logger.error { "Save failed: conversation=$conversationId, error=${e.message}" } }
    }

    /**
     * Returns the name of this strategy.
     */
    override fun getName(): String = "end-of-stream"
} 