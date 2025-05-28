package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import com.sgrecu.homeassignment.chat.strategy.MessageSaveStrategy
import com.sgrecu.homeassignment.metrics.MetricsService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

/**
 * Component responsible for persisting messages to the database.
 * Uses the configured MessageSaveStrategy for the actual persistence.
 */
@Component
class MessagePersister(
    private val configuredMessageSaveStrategy: MessageSaveStrategy, private val metricsService: MetricsService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Saves a message stream to the database.
     *
     * @param contentFlux The content to save
     * @param conversationId The conversation ID to associate with the content
     * @param role The role of the message (user, assistant, system)
     * @return A completion signal when the operation is done
     */
    fun saveMessage(
        contentFlux: Flux<String>, conversationId: UUID, role: MessageRoleEnum
    ): Mono<Void> =
        configuredMessageSaveStrategy.save(contentFlux, conversationId, role).publishOn(Schedulers.boundedElastic())
            .doOnError { error ->
                metricsService.recordMessagePersistenceFailure(conversationId, role, error)
                logger.error(
                    "Message persistence failed: conversation={}, role={}, error={}",
                    conversationId,
                    role,
                    error.message,
                    error
                )
            }.onErrorComplete().then()
} 