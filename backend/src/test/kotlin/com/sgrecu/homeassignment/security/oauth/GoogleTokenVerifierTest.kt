package com.sgrecu.homeassignment.security.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.sgrecu.homeassignment.security.model.User
import com.sgrecu.homeassignment.security.model.UserPrincipal
import com.sgrecu.homeassignment.security.service.UserService
import com.sgrecu.homeassignment.security.service.UserService.UserWithRoles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

/**
 * Tests for GoogleTokenVerifier using a testable subclass that exposes the verifier
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleTokenVerifierTest {

    @Mock
    private lateinit var properties: GoogleAuthProperties

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier

    private lateinit var tokenVerifier: TestableGoogleTokenVerifier

    private val testNow = Instant.parse("2023-01-01T12:00:00Z")
    private val testTokenString = "valid.google.token"
    private val testGoogleUserId = "google-user-123"
    private val testEmail = "test@example.com"
    private val testName = "Test User"
    private val testProvider = "google"

    /**
     * Testable subclass that allows us to provide our own verifier
     * without using reflection
     */
    class TestableGoogleTokenVerifier(
        properties: GoogleAuthProperties, userService: UserService, val testVerifier: GoogleIdTokenVerifier
    ) : GoogleTokenVerifier(properties, userService) {

        // Override the property to return our mock verifier
        override val verifier: GoogleIdTokenVerifier
            get() = testVerifier
    }

    @BeforeEach
    fun setup() {
        // Mock properties
        `when`(properties.clientId).thenReturn("test-client-id")

        // Create test verifier with injected mocks
        tokenVerifier = TestableGoogleTokenVerifier(properties, userService, googleIdTokenVerifier)
    }

    @Test
    fun `verifyIdToken should return empty when token is blank`() {
        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken("")).verifyComplete()

        // Verify the verifier wasn't called
        verify(googleIdTokenVerifier, Mockito.never()).verify(Mockito.anyString())
    }

    @Test
    fun `verifyIdToken should return empty when token verification fails`() {
        // Given
        `when`(googleIdTokenVerifier.verify(testTokenString)).thenReturn(null)

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString)).verifyComplete()
    }

    @Test
    fun `verifyIdToken should return empty when token verification throws exception`() {
        // Given
        `when`(googleIdTokenVerifier.verify(testTokenString)).thenThrow(RuntimeException("Token verification failed"))

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString)).verifyComplete()
    }

    @Test
    fun `verifyIdToken should return empty when email is missing from payload`() {
        // Given
        val mockToken = Mockito.mock(GoogleIdToken::class.java)
        val mockPayload = Mockito.mock(GoogleIdToken.Payload::class.java)

        `when`(mockToken.payload).thenReturn(mockPayload)
        `when`(mockPayload.subject).thenReturn(testGoogleUserId)
        `when`(mockPayload["email"]).thenReturn(null) // Missing email
        `when`(googleIdTokenVerifier.verify(testTokenString)).thenReturn(mockToken)

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString)).verifyComplete()
    }

    @Test
    fun `verifyIdToken should return user principal when token is valid`() {
        // Given
        val mockToken = Mockito.mock(GoogleIdToken::class.java)
        val mockPayload = Mockito.mock(GoogleIdToken.Payload::class.java)

        `when`(mockToken.payload).thenReturn(mockPayload)
        `when`(mockPayload.subject).thenReturn(testGoogleUserId)
        `when`(mockPayload["email"]).thenReturn(testEmail)
        `when`(mockPayload["name"]).thenReturn(testName)

        `when`(googleIdTokenVerifier.verify(testTokenString)).thenReturn(mockToken)

        val user = User(
            id = 1L,
            externalId = testGoogleUserId,
            email = testEmail,
            displayName = testName,
            provider = testProvider,
            createdAt = testNow,
            updatedAt = testNow
        )

        val userWithRoles = UserWithRoles(
            user = user, roles = setOf("USER")
        )

        `when`(
            userService.findOrCreateUser(
                testGoogleUserId, testEmail, testName, "google"
            )
        ).thenReturn(Mono.just(userWithRoles))

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString))
            .expectNextMatches { principal: UserPrincipal ->
                principal.externalId == testGoogleUserId && principal.email == testEmail && principal.roles.contains("USER")
            }.verifyComplete()
    }

    @Test
    fun `verifyIdToken should use email as name when name is missing from payload`() {
        // Given
        val mockToken = Mockito.mock(GoogleIdToken::class.java)
        val mockPayload = Mockito.mock(GoogleIdToken.Payload::class.java)

        `when`(mockToken.payload).thenReturn(mockPayload)
        `when`(mockPayload.subject).thenReturn(testGoogleUserId)
        `when`(mockPayload["email"]).thenReturn(testEmail)
        `when`(mockPayload["name"]).thenReturn(null) // Missing name

        `when`(googleIdTokenVerifier.verify(testTokenString)).thenReturn(mockToken)

        val user = User(
            id = 1L,
            externalId = testGoogleUserId,
            email = testEmail,
            displayName = testEmail, // Email used as display name
            provider = testProvider,
            createdAt = testNow,
            updatedAt = testNow
        )

        val userWithRoles = UserWithRoles(
            user = user, roles = setOf("USER")
        )

        // Should use email as name
        `when`(
            userService.findOrCreateUser(
                testGoogleUserId, testEmail, testEmail, "google"
            )
        ).thenReturn(Mono.just(userWithRoles))

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString))
            .expectNextMatches { principal: UserPrincipal ->
                principal.externalId == testGoogleUserId && principal.email == testEmail && principal.roles.contains("USER")
            }.verifyComplete()
    }

    @Test
    fun `verifyIdToken should handle error in user service`() {
        // Given
        val mockToken = Mockito.mock(GoogleIdToken::class.java)
        val mockPayload = Mockito.mock(GoogleIdToken.Payload::class.java)

        `when`(mockToken.payload).thenReturn(mockPayload)
        `when`(mockPayload.subject).thenReturn(testGoogleUserId)
        `when`(mockPayload["email"]).thenReturn(testEmail)
        `when`(mockPayload["name"]).thenReturn(testName)

        `when`(googleIdTokenVerifier.verify(testTokenString)).thenReturn(mockToken)

        // Simulate error in user service
        `when`(
            userService.findOrCreateUser(
                testGoogleUserId, testEmail, testName, "google"
            )
        ).thenReturn(Mono.error(RuntimeException("Failed to create user")))

        // When & Then
        StepVerifier.create(tokenVerifier.verifyIdToken(testTokenString))
            .verifyComplete() // Should gracefully handle the error
    }
} 