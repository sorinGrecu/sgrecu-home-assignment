package com.sgrecu.homeassignment.security.oauth

import com.sgrecu.homeassignment.security.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Simple test for GoogleTokenVerifier focusing on boundary cases
 */
@ExtendWith(SpringExtension::class)
class GoogleTokenVerifierSimpleTest {

    private lateinit var properties: GoogleAuthProperties
    private lateinit var userService: UserService
    private lateinit var tokenVerifier: GoogleTokenVerifier

    private val testTokenString = "valid.google.token"
    private val testBlankToken = ""

    @BeforeEach
    fun setup() {
        properties = Mockito.mock(GoogleAuthProperties::class.java)
        userService = Mockito.mock(UserService::class.java)

        Mockito.`when`(properties.clientId).thenReturn("test-client-id")

        tokenVerifier = GoogleTokenVerifier(properties, userService)
    }

    @Test
    fun `verifyIdToken should return empty when token is blank`() {
        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testBlankToken)).verifyComplete()
    }

    @Test
    fun `verifyIdToken should handle empty user result`() {
        Mockito.`when`(
            userService.findOrCreateUser(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
            )
        ).thenReturn(Mono.empty())

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString)).verifyComplete()
    }
} 