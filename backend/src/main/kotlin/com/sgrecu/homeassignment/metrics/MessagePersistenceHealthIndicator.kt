package com.sgrecu.homeassignment.metrics

import mu.KotlinLogging
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

/**
 * Health indicator that monitors message persistence failures.
 * Transitions to DOWN state if too many persistence failures occur within a window.
 */
@Component
class MessagePersistenceHealthIndicator(private val metricsService: MetricsService) : HealthIndicator {
    private val logger = KotlinLogging.logger {}

    private val warningThreshold = 5L
    private val criticalThreshold = 20L

    override fun health(): Health {
        val failureCount = metricsService.getMessagePersistenceFailureCount()
        val lostMessages = metricsService.getLostMessagesCount()

        return when {
            failureCount >= criticalThreshold -> {
                logger.error(
                    "Critical message persistence failure rate detected: {} failures, {} lost messages",
                    failureCount,
                    lostMessages
                )
                Health.down().withDetail("failures", failureCount).withDetail("lostMessages", lostMessages)
                    .withDetail("status", "CRITICAL").build()
            }

            failureCount >= warningThreshold -> {
                logger.warn(
                    "Elevated message persistence failure rate detected: {} failures, {} lost messages",
                    failureCount,
                    lostMessages
                )
                Health.status("WARNING").withDetail("failures", failureCount).withDetail("lostMessages", lostMessages)
                    .withDetail("status", "WARNING").build()
            }

            else -> {
                Health.up().withDetail("failures", failureCount).withDetail("lostMessages", lostMessages)
                    .withDetail("status", "OK").build()
            }
        }
    }
} 