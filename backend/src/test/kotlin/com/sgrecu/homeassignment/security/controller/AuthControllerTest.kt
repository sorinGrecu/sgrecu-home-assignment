package com.sgrecu.homeassignment.security.controller

import com.sgrecu.homeassignment.security.jwt.JwtTokenProvider
import com.sgrecu.homeassignment.security.model.UserPrincipal
import com.sgrecu.homeassignment.security.oauth.GoogleTokenVerifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthControllerTest {

    private lateinit var googleTokenVerifier: GoogleTokenVerifier
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authController: AuthController

    private val testGoogleToken = "valid.google.token"
    private val testJwtToken = "generated.jwt.token"
    private val testUserId = "google-user-123"
    private val testEmail = "test@example.com"

    @BeforeEach
    fun setUp() {
        googleTokenVerifier = Mockito.mock(GoogleTokenVerifier::class.java)
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        authController = AuthController(googleTokenVerifier, jwtTokenProvider)
    }

    @Test
    fun `authenticateWithGoogle should return JWT response for valid token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)
        val userPrincipal = UserPrincipal(
            externalId = testUserId, email = testEmail, roles = setOf("USER")
        )

        whenever(googleTokenVerifier.verifyIdToken(eq(testGoogleToken))).thenReturn(Mono.just(userPrincipal))
        whenever(jwtTokenProvider.generateToken(any())).thenReturn(testJwtToken)

        // When
        val result = authController.authenticateWithGoogle(tokenRequest)

        // Then
        StepVerifier.create(result).expectNext(AuthController.JwtResponse(accessToken = testJwtToken)).verifyComplete()
    }

    @Test
    fun `authenticateWithGoogle should return error for blank token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest("")

        // When
        val result = authController.authenticateWithGoogle(tokenRequest)

        // Then
        StepVerifier.create(result).expectError(IllegalArgumentException::class.java).verify()
    }

    @Test
    fun `authenticateWithGoogle should return error for invalid token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)

        whenever(googleTokenVerifier.verifyIdToken(eq(testGoogleToken))).thenReturn(Mono.empty())

        // When
        val result = authController.authenticateWithGoogle(tokenRequest)

        // Then
        StepVerifier.create(result).expectError(IllegalAccessException::class.java).verify()
    }

    @Test
    fun `authenticateWithGoogle should propagate errors from token verifier`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)
        val exception = RuntimeException("Token verification error")

        whenever(googleTokenVerifier.verifyIdToken(eq(testGoogleToken))).thenReturn(Mono.error(exception))

        // When
        val result = authController.authenticateWithGoogle(tokenRequest)

        // Then
        StepVerifier.create(result).expectErrorMatches { it == exception }.verify()
    }
} 