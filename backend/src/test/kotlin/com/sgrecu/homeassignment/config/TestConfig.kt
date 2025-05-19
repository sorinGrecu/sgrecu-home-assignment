package com.sgrecu.homeassignment.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Legacy test configuration providing transaction management.
 *
 * Note: Database connection configuration has been moved to TestDatabaseConfig
 * and the consolidated DatabaseProperties approach.
 */
@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    @Profile("test")
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }

    @Bean
    @Primary
    @Profile("test")
    fun transactionalOperator(transactionManager: ReactiveTransactionManager): TransactionalOperator {
        return TransactionalOperator.create(transactionManager)
    }
} 