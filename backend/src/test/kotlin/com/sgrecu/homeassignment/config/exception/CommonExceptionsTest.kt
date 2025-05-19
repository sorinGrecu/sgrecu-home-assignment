package com.sgrecu.homeassignment.config.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class CommonExceptionsTest {

    @Test
    fun `AuthenticationRequiredException should have UNAUTHORIZED status`() {
        // When
        val exception = AuthenticationRequiredException()
        
        // Then
        assertEquals("User authentication required", exception.message)
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }
    
    @Test
    fun `AuthenticationRequiredException should accept custom message`() {
        // When
        val customMessage = "Custom auth message"
        val exception = AuthenticationRequiredException(customMessage)
        
        // Then
        assertEquals(customMessage, exception.message)
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }

    @Test
    fun `ResourceNotFoundException should format message with resource type and id`() {
        // When
        val exception = ResourceNotFoundException("User", "123")
        
        // Then
        assertEquals("Resource of type User with ID 123 not found", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
} 