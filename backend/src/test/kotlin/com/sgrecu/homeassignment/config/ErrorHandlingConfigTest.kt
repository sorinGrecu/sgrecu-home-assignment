package com.sgrecu.homeassignment.config

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.web.WebProperties
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ErrorHandlingConfigTest {

    @Test
    fun `should create Resources bean`() {
        // Given
        val config = ErrorHandlingConfig()

        // When
        val resources = config.resources()

        // Then
        assertNotNull(resources)
        assertEquals(WebProperties.Resources::class, resources::class)
    }
} 