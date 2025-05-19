package com.sgrecu.homeassignment.config

import mu.KotlinLogging
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.TestPropertySource

/**
 * Base test configuration that loads the application-test.yml properties
 * for all test cases.
 */
@TestConfiguration
@Profile("test")
@TestPropertySource(locations = ["classpath:application-test.yml"])
class TestBaseConfig {
    private val logger = KotlinLogging.logger {}

    init {
        logger.info("Initializing test configuration with application-test.yml properties")
    }
} 