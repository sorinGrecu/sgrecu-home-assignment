package com.sgrecu.homeassignment.config.properties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration to enable the DatabaseProperties.
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties::class)
class DatabasePropertiesConfig 