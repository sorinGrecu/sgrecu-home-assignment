package com.sgrecu.homeassignment.security.jwt

import com.sgrecu.homeassignment.security.model.User
import com.sgrecu.homeassignment.security.service.UserService
import com.sgrecu.homeassignment.security.service.UserService.UserWithRoles
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

@DisplayName("JwtAuthenticationConverter Tests")
class JwtAuthenticationConverterTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userService: UserService
    private lateinit var jwtAuthConverter: JwtAuthenticationConverter
    private val testNow = Instant.parse("2023-01-01T12:00:00Z")

    @BeforeEach
    fun setup() {
        jwtTokenProvider = mockk()
        userService = mockk()
        jwtAuthConverter = JwtAuthenticationConverter(jwtTokenProvider, userService)
    }

    @Test
    fun `convert returns authentication token for valid JWT token`() {
        // Given
        val token = "valid.jwt.token"
        val userId = "user-123"
        val email = "user@example.com"
        val provider = "google"
        val roles = setOf("ROLE_USER")

        val user = User(
            id = 1L,
            externalId = userId,
            provider = provider,
            email = email,
            displayName = "Test User",
            createdAt = testNow,
            updatedAt = testNow
        )

        val userWithRoles = UserWithRoles(user, roles)

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.just(userId)
        every { userService.getUserByExternalId(userId, provider) } returns Mono.just(userWithRoles)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectNextMatches { auth ->
                auth is UsernamePasswordAuthenticationToken && auth.credentials.toString() == token && auth.authorities.size == roles.size && auth.authorities.any { it.authority == "ROLE_USER" }
            }.verifyComplete()
    }

    @Test
    fun `convert returns empty for invalid JWT token`() {
        // Given
        val token = "invalid.jwt.token"

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(false)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).verifyComplete()
    }

    @Test
    fun `convert returns empty for valid token but missing user ID`() {
        // Given
        val token = "valid.jwt.token"

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.just("")

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).verifyComplete()
    }

    @Test
    fun `convert returns error for valid token but user not in database`() {
        // Given
        val token = "valid.jwt.token"
        val userId = "user-123"
        val provider = "google"

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.just(userId)
        every { userService.getUserByExternalId(userId, provider) } returns Mono.empty()

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectError(UsernameNotFoundException::class.java).verify()
    }

    @Test
    fun `convert correctly sets UserPrincipal and authorities in authentication token`() {
        // Given
        val token = "valid.jwt.token"
        val userId = "user-123"
        val email = "user@example.com"
        val provider = "google"
        val roles = setOf("ROLE_USER", "ROLE_ADMIN")

        val user = User(
            id = 1L,
            externalId = userId,
            provider = provider,
            email = email,
            displayName = "Test User",
            createdAt = testNow,
            updatedAt = testNow
        )

        val userWithRoles = UserWithRoles(user, roles)

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.just(userId)
        every { userService.getUserByExternalId(userId, provider) } returns Mono.just(userWithRoles)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectNextMatches { auth ->
                val principal = auth.principal
                auth is UsernamePasswordAuthenticationToken && auth.credentials.toString() == token && auth.authorities.size == roles.size && auth.authorities.any { it.authority == "ROLE_USER" } && auth.authorities.any { it.authority == "ROLE_ADMIN" } && principal.toString()
                    .contains(userId) && principal.toString().contains(email)
            }.verifyComplete()
    }

    @Test
    fun `convert handles error from token validation`() {
        // Given
        val token = "valid.jwt.token"
        val exception = RuntimeException("Validation error")

        every { jwtTokenProvider.validateToken(token) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectError(RuntimeException::class.java).verify()

        verify(exactly = 0) { jwtTokenProvider.getUserIdFromToken(any()) }
        verify(exactly = 0) { userService.getUserByExternalId(any(), any()) }
    }

    @Test
    fun `convert handles error from getUserIdFromToken`() {
        // Given
        val token = "valid.jwt.token"
        val exception = RuntimeException("Token parsing error")

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectError(RuntimeException::class.java).verify()

        verify(exactly = 0) { userService.getUserByExternalId(any(), any()) }
    }

    @Test
    fun `convert handles user service throwing specific exception`() {
        // Given
        val token = "valid.jwt.token"
        val userId = "user-123"
        val provider = "google"
        val exception = IllegalStateException("User service error")

        every { jwtTokenProvider.validateToken(token) } returns Mono.just(true)
        every { jwtTokenProvider.getUserIdFromToken(token) } returns Mono.just(userId)
        every { userService.getUserByExternalId(userId, provider) } returns Mono.error(exception)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(token)).expectError(IllegalStateException::class.java).verify()
    }

    @Test
    fun `convert handles empty token gracefully`() {
        // Given
        val emptyToken = ""

        // Set up mock for empty token
        every { jwtTokenProvider.validateToken(emptyToken) } returns Mono.just(false)

        // When/Then
        StepVerifier.create(jwtAuthConverter.convert(emptyToken)).verifyComplete()

        verify { jwtTokenProvider.validateToken(emptyToken) }
        verify(exactly = 0) { userService.getUserByExternalId(any(), any()) }
    }
} 