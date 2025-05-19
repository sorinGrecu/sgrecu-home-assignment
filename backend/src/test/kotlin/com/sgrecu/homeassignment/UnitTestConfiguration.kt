package com.sgrecu.homeassignment

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Test configuration for unit tests
 * Provides mock beans that can be used in unit tests
 */
@TestConfiguration
@Profile("test")
class UnitTestConfiguration {

    @Bean
    fun clock(): Clock {
        return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"))
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
} 