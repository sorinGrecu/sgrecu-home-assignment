package com.sgrecu.homeassignment.metrics

import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized service for managing application metrics.
 * This service provides methods for recording various metrics
 * across the application for monitoring purposes.
 */
@Service
class MetricsService(
    private val meterRegistry: MeterRegistry,
    private val resetIntervalMs: Long = DEFAULT_RESET_INTERVAL_MS,
    private val clock: Clock
) {
    private val logger = KotlinLogging.logger {}

    private val messagePersistenceFailureCounter =
        Counter.builder("message.persistence.failures").description("Number of message persistence failures")
            .register(meterRegistry)

    private val lostMessagesCounter = AtomicLong(0)
    private val recentFailuresCounter = AtomicLong(0)
    private val lastResetTime = AtomicLong(clock.millis())

    init {
        meterRegistry.gauge("message.persistence.lost_messages", lostMessagesCounter)
        meterRegistry.gauge("message.persistence.recent_failures", recentFailuresCounter)
    }

    /**
     * Records a message persistence failure with detailed tags for analysis.
     *
     * @param conversationId The ID of the conversation
     * @param role The role of the message (user, assistant, system)
     * @param error The exception that occurred
     */
    fun recordMessagePersistenceFailure(conversationId: UUID, role: MessageRoleEnum, error: Throwable) {
        messagePersistenceFailureCounter.increment()
        lostMessagesCounter.incrementAndGet()

        checkAndResetRecentFailures()
        recentFailuresCounter.incrementAndGet()

        Counter.builder("message.persistence.failure.details").tag("role", role.name)
            .tag("error_type", error.javaClass.simpleName).tag("error_message", error.message?.take(50) ?: "unknown")
            .register(meterRegistry).increment()

        logger.warn(
            "Data loss detected! Conversation: {}, Role: {}, Error: {}",
            conversationId,
            role,
            error.javaClass.simpleName
        )
    }

    /**
     * Gets the total count of persistence failures since application start.
     *
     * @return The total number of persistence failures
     */
    fun getMessagePersistenceFailureCount(): Long {
        return messagePersistenceFailureCounter.count().toLong()
    }

    /**
     * Gets the count of lost messages since application start.
     *
     * @return The number of lost messages
     */
    fun getLostMessagesCount(): Long {
        return lostMessagesCounter.get()
    }

    /**
     * Gets the count of recent failures since the last reset.
     * Useful for monitoring short-term error spikes.
     *
     * @return The number of recent failures
     */
    fun getRecentFailuresCount(): Long {
        return recentFailuresCounter.get()
    }

    /**
     * Gets the time of the last counter reset in milliseconds since epoch.
     *
     * @return The time of the last reset
     */
    fun getLastResetTime(): Long {
        return lastResetTime.get()
    }

    /**
     * Manually resets the recent failures counter.
     * Primarily intended for testing or administrative purposes.
     */
    fun resetRecentFailures() {
        lastResetTime.set(clock.millis())
        recentFailuresCounter.set(0)
    }

    /**
     * Checks if the reset interval has elapsed and resets the counter if needed.
     */
    private fun checkAndResetRecentFailures() {
        val now = clock.millis()
        val lastReset = lastResetTime.get()

        if (now - lastReset > resetIntervalMs) {
            if (lastResetTime.compareAndSet(lastReset, now)) {
                recentFailuresCounter.set(0)
            }
        }
    }

    companion object {
        const val DEFAULT_RESET_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }
} 