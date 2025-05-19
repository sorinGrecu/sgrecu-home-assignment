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
    override fun save(contents: Flux<String>, conversationId: UUID, role: MessageRoleEnum): Mono<Void> {
        logger.debug { "Starting collection for conversation $conversationId" }

        val contentBuilder = StringBuilder()

        return contentFilter.filterContent(contents).doOnNext { token ->
            contentBuilder.append(token)
        }.then(Mono.defer {
            val fullContent = contentBuilder.toString()

            if (fullContent.isEmpty()) {
                logger.debug { "No content after filtering: conversation=$conversationId" }
                return@defer Mono.empty<Void>()
            }

            logger.debug {
                "Saving message: conversation=$conversationId, length=${fullContent.length}"
            }

            messageRepository.save(
                Message(conversationId = conversationId, role = role, content = fullContent)
            ).doOnSuccess { message ->
                logger.debug { "Message saved: id=${message.id}, conversation=$conversationId" }
            }.then()
        }).doOnError { error ->
            logger.error {
                "Save failed: conversation=$conversationId, error=${error.message}"
            }
        }
    }

    /**
     * Returns the name of this strategy.
     */
    override fun getName(): String = "end-of-stream"
} 