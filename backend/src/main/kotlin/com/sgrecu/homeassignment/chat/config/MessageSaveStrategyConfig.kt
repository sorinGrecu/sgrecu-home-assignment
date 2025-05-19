package com.sgrecu.homeassignment.chat.config

import com.sgrecu.homeassignment.chat.strategy.MessageSaveStrategy
import com.sgrecu.homeassignment.config.AppProperties
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for message save strategies.
 * This configuration class selects the appropriate message save strategy
 * based on the configured strategy name.
 */
@Configuration
class MessageSaveStrategyConfig {
    private val logger = KotlinLogging.logger {}

    /**
     * Provides the appropriate MessageSaveStrategy based on the configured strategy name.
     *
     * @param strategies All available MessageSaveStrategy implementations
     * @param appProperties Application properties containing the save strategy configuration
     * @return The selected MessageSaveStrategy
     */
    @Bean
    fun configuredMessageSaveStrategy(
        strategies: List<MessageSaveStrategy>, appProperties: AppProperties
    ): MessageSaveStrategy {
        val configuredStrategyName = appProperties.saveStrategy
        val availableStrategies = strategies.associate { it.getName() to it.javaClass.simpleName }
        logger.info { "Registered message save strategies: $availableStrategies" }

        val selectedStrategy = strategies.find { it.getName() == configuredStrategyName }

        return if (selectedStrategy != null) {
            logger.info { "Using '$configuredStrategyName' as the message save strategy" }
            selectedStrategy
        } else {
            val defaultStrategy = strategies.first()
            logger.warn {
                "Configured strategy '$configuredStrategyName' not found. Using default strategy '${defaultStrategy.getName()}' instead."
            }
            defaultStrategy
        }
    }
} 