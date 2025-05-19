package com.sgrecu.homeassignment.config.util

import com.sgrecu.homeassignment.config.properties.DatabaseProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Utility component that resolves database configuration based on the active environment profile.
 * Centralizes environment detection logic to avoid duplication across database configuration classes.
 */
@Component
class DatabaseEnvironmentResolver(
    private val environment: Environment
) {

    /**
     * Determines the active database environment based on Spring profiles
     */
    fun getActiveEnvironment(): DatabaseEnvironment {
        val activeProfiles = environment.activeProfiles

        return when {
            activeProfiles.contains("test") -> DatabaseEnvironment.TEST
            activeProfiles.contains("dev") -> DatabaseEnvironment.DEV
            activeProfiles.contains("staging") -> DatabaseEnvironment.STAGING
            activeProfiles.contains("prod") -> DatabaseEnvironment.PROD
            else -> DatabaseEnvironment.PROD
        }
    }

    /**
     * Creates an H2 JDBC URL based on the active environment
     */
    fun createH2JdbcUrl(properties: DatabaseProperties): String {
        val h2Config = properties.h2

        return when (getActiveEnvironment()) {
            DatabaseEnvironment.TEST -> "jdbc:h2:mem:${h2Config.databaseName}"
            else -> "jdbc:h2:file:${h2Config.databaseName}"
        }
    }

    /**
     * Creates an H2 R2DBC URL based on the active environment
     */
    fun createH2R2dbcUrl(properties: DatabaseProperties): String {
        val h2Config = properties.h2

        return when (getActiveEnvironment()) {
            DatabaseEnvironment.TEST -> "r2dbc:h2:mem:///${h2Config.databaseName}"
            else -> "r2dbc:h2:file:///${h2Config.databaseName}"
        }
    }

    /**
     * Creates a PostgreSQL JDBC URL using the provided properties
     */
    fun createPostgresJdbcUrl(properties: DatabaseProperties): String {
        val pgConfig = properties.postgres
        return "jdbc:postgresql://${pgConfig.host}:${pgConfig.port}/${pgConfig.database}"
    }

    /**
     * Creates a PostgreSQL R2DBC URL using the provided properties
     */
    fun createPostgresR2dbcUrl(properties: DatabaseProperties): String {
        val pgConfig = properties.postgres
        return "r2dbc:postgresql://${pgConfig.host}:${pgConfig.port}/${pgConfig.database}"
    }
}

/**
 * Enum representing the possible database environments
 */
enum class DatabaseEnvironment {
    TEST, DEV, STAGING, PROD
} 