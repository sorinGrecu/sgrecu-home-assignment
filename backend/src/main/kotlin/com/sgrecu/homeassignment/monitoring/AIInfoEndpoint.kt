package com.sgrecu.homeassignment.monitoring

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

/**
 * Custom actuator endpoint exposing AI model information and queue depth.
 */
@Component
@Endpoint(id = "aiinfo")
class AIInfoEndpoint(
    private val applicationContext: ApplicationContext
) {
    private val activeRequests = AtomicInteger(0)

    /**
     * Increment the active requests counter.
     */
    fun incrementActiveRequests() {
        activeRequests.incrementAndGet()
    }

    /**
     * Decrement the active requests counter.
     */
    fun decrementActiveRequests() {
        activeRequests.decrementAndGet()
    }

    /**
     * Retrieves AI model information and queue depth.
     * Accessed via /actuator/aiinfo
     */
    @ReadOperation
    fun info(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "activeRequests" to activeRequests.get(),
                "model" to getModelInfo(),
                "applicationInfo" to getApplicationInfo()
            )
        )
    }

    /**
     * Gets the current AI model information.
     */
    private fun getModelInfo(): Map<String, Any> {
        val aiConfig = applicationContext.environment.getProperty("spring.ai.ollama.chat.options.model") ?: "unknown"
        val baseUrl = applicationContext.environment.getProperty("spring.ai.ollama.base-url") ?: "unknown"
        val temperature =
            applicationContext.environment.getProperty("spring.ai.ollama.chat.options.temperature") ?: "unknown"

        return mapOf(
            "name" to aiConfig, "baseUrl" to baseUrl, "temperature" to temperature
        )
    }

    /**
     * Gets basic application information.
     */
    private fun getApplicationInfo(): Map<String, Any> {
        return mapOf(
            "name" to (applicationContext.environment.getProperty("spring.application.name") ?: "unknown"),
            "profiles" to (applicationContext.environment.activeProfiles.toList())
        )
    }
} 