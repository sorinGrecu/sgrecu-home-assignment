package com.sgrecu.homeassignment.config

import com.sgrecu.homeassignment.config.properties.DatabaseProperties
import com.sgrecu.homeassignment.config.util.DatabaseEnvironment
import com.sgrecu.homeassignment.config.util.DatabaseEnvironmentResolver
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Consolidated database configuration that handles both R2DBC connection factories
 * and Flyway migrations across different environments.
 */
@Configuration
@EnableR2dbcAuditing
@EnableTransactionManagement
class DatabaseConnectionConfig(
    private val databaseProperties: DatabaseProperties, private val environmentResolver: DatabaseEnvironmentResolver
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Configures and runs Flyway migrations appropriate for the current environment.
     * This bean must be initialized before R2DBC connection factories.
     */
    @Bean
    @Primary
    fun flyway(): Flyway {
        val environment = environmentResolver.getActiveEnvironment()
        val flywayConfig = databaseProperties.flyway

        val jdbcUrl: String
        val username: String
        val password: String
        val migrationLocations: Array<String>

        when (environment) {
            DatabaseEnvironment.TEST, DatabaseEnvironment.DEV -> {
                jdbcUrl = environmentResolver.createH2JdbcUrl(databaseProperties)
                username = databaseProperties.h2.username
                password = databaseProperties.h2.password
                migrationLocations = arrayOf("classpath:db/migration/h2")
                logger.info { "Initializing Flyway for ${environment.name.lowercase()} with URL: $jdbcUrl" }
            }

            DatabaseEnvironment.STAGING, DatabaseEnvironment.PROD -> {
                jdbcUrl = environmentResolver.createPostgresJdbcUrl(databaseProperties)
                username = databaseProperties.postgres.username
                password = databaseProperties.postgres.password
                migrationLocations = arrayOf("classpath:db/migration/postgresql")
                logger.info { "Initializing Flyway for ${environment.name.lowercase()} with URL: $jdbcUrl" }

                if (jdbcUrl.isBlank()) {
                    throw IllegalStateException(
                        "No explicit JDBC URL provided for Flyway migrations in ${environment.name.lowercase()} environment. " + 
                        "Please set app.database.postgres configuration explicitly."
                    )
                }
            }
        }

        logger.info { "Using migration locations: ${migrationLocations.joinToString()} for ${environment.name}" }
        
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations(*migrationLocations)
            .baselineOnMigrate(flywayConfig.baselineOnMigrate)
            .load()

        flyway.migrate()
        return flyway
    }

    /**
     * Creates the appropriate ConnectionFactory for the current environment.
     * This bean depends on Flyway to ensure migrations run before connections are established.
     */
    @Bean
    @Primary
    @DependsOn("flyway")
    fun connectionFactory(): ConnectionFactory {
        return when (val environment = environmentResolver.getActiveEnvironment()) {
            DatabaseEnvironment.TEST, DatabaseEnvironment.DEV -> createH2ConnectionFactory(environment)
            DatabaseEnvironment.STAGING, DatabaseEnvironment.PROD -> createPostgresConnectionFactory()
        }
    }

    /**
     * Creates an R2DBC transaction manager for the configured connection factory.
     */
    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory).apply {
            isEnforceReadOnly = false
        }
    }

    /**
     * Creates a transactional operator for managing reactive transactions.
     */
    @Bean
    fun transactionalOperator(transactionManager: ReactiveTransactionManager): TransactionalOperator {
        return TransactionalOperator.create(transactionManager)
    }

    /**
     * Creates a database client from the connection factory.
     */
    @Bean
    fun databaseClient(connectionFactory: ConnectionFactory): DatabaseClient {
        return DatabaseClient.create(connectionFactory)
    }

    /**
     * Helper method to create an H2 connection factory for the given environment.
     */
    private fun createH2ConnectionFactory(environment: DatabaseEnvironment): ConnectionFactory {
        val h2Config = databaseProperties.h2
        val url = environmentResolver.createH2R2dbcUrl(databaseProperties)

        logger.info { "Creating H2 connection factory for ${environment.name.lowercase()} with URL: $url" }

        val options = ConnectionFactoryOptions.builder().from(ConnectionFactoryOptions.parse(url))
            .option(ConnectionFactoryOptions.USER, h2Config.username)
            .option(ConnectionFactoryOptions.PASSWORD, h2Config.password).build()

        return ConnectionFactories.get(options)
    }

    /**
     * Helper method to create a PostgreSQL connection factory.
     */
    private fun createPostgresConnectionFactory(): ConnectionFactory {
        val pgConfig = databaseProperties.postgres
        val url = environmentResolver.createPostgresR2dbcUrl(databaseProperties)

        logger.info { "Creating PostgreSQL connection factory with URL: $url" }

        val options = ConnectionFactoryOptions.builder().from(ConnectionFactoryOptions.parse(url))
            .option(ConnectionFactoryOptions.USER, pgConfig.username)
            .option(ConnectionFactoryOptions.PASSWORD, pgConfig.password).build()

        return ConnectionFactories.get(options)
    }
} 