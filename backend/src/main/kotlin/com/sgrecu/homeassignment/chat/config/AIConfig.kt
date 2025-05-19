package com.sgrecu.homeassignment.chat.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Configuration properties for AI features and behavior.
 * Centralizes AI-related configurations for easier maintenance.
 */
@ConfigurationProperties(prefix = "ai")
@Validated
data class AIConfig(
    /**
     * Configuration for AI thinking mode
     */
    val thinking: ThinkingConfig = ThinkingConfig()
)

/**
 * Configuration for thinking mode feature.
 */
data class ThinkingConfig(
    /**
     * Whether thinking mode feature is enabled
     */
    val enabled: Boolean = true,

    /**
     * Start tag for thinking mode section
     */
    @field:NotBlank val startTag: String = "<think>",

    /**
     * End tag for thinking mode section
     */
    @field:NotBlank val endTag: String = "</think>"
) 