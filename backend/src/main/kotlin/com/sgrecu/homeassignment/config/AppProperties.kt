package com.sgrecu.homeassignment.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Configuration properties for the application.
 * This class centralizes all application-specific configuration properties.
 */
@ConfigurationProperties(prefix = "app")
@Validated
data class AppProperties(
    /**
     * The message save strategy to use.
     * Supported values: "end-of-stream"
     */
    @field:NotBlank @field:Pattern(
        regexp = "end-of-stream", message = "Save strategy must be either 'end-of-stream' or a future implementation"
    ) val saveStrategy: String = "end-of-stream"
)
