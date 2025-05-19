package com.sgrecu.homeassignment.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Centralized database configuration properties that serve both R2DBC and Flyway.
 * Provides a single configuration source for database connection parameters.
 */
@ConfigurationProperties(prefix = "app.database")
data class DatabaseProperties(
    /** Core database connection parameters used by both R2DBC and Flyway */
    val common: CommonDbConfig = CommonDbConfig(),

    /** H2 database specific configuration options */
    val h2: H2Config = H2Config(),

    /** PostgreSQL specific configuration options */
    val postgres: PostgresConfig = PostgresConfig(),

    /** Flyway migration configuration settings */
    val flyway: FlywayConfig = FlywayConfig(),

    /** R2DBC connection and pool configuration settings */
    val r2dbc: R2dbcConfig = R2dbcConfig()
) {
    /**
     * Core database connection parameters shared across different database configurations
     */
    data class CommonDbConfig(
        val host: String = "localhost",
        val port: Int = 5432,
        val database: String = "spring_ai",
        val username: String = "postgres",
        val password: String = "postgres"
    )

    /**
     * H2 database configuration options including storage mode and credentials
     */
    data class H2Config(
        val mode: String = "file",
        val databaseName: String = "./data/chatdb",
        val username: String = "sa",
        val password: String = ""
    )

    /**
     * PostgreSQL connection parameters including schema selection
     */
    data class PostgresConfig(
        val host: String = "localhost",
        val port: Int = 5432,
        val database: String = "spring_ai",
        val username: String = "postgres",
        val password: String = "postgres",
        val schema: String = "public"
    )

    /**
     * Flyway migration configuration for database versioning and schema evolution
     */
    data class FlywayConfig(
        val locations: List<String> = listOf("classpath:db/migration"), val baselineOnMigrate: Boolean = true
    )

    /**
     * R2DBC connection pool settings for managing database connections
     */
    data class R2dbcConfig(
        val poolEnabled: Boolean = true,
        val initialSize: Int = 5,
        val maxSize: Int = 10,
        val maxIdleTime: String = "30m",
        val validationQuery: String = "SELECT 1"
    )
} 