package com.sgrecu.homeassignment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.Clock

/**
 * Main Spring Boot application class for the Spring AI application.
 * Enables various Spring features through annotations.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableR2dbcRepositories
@EnableTransactionManagement
@EnableScheduling
class SpringAiApplication {
    
    /**
     * Provides a system UTC Clock bean that can be injected throughout the application.
     * This centralizes time management and makes testing easier by allowing Clock mocking.
     * Using UTC avoids issues with DST jumps breaking "recent failure" calculations.
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<SpringAiApplication>(*args) {
        setLogStartupInfo(true)
    }
}
