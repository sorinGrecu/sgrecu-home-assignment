package com.sgrecu.homeassignment.security.controller

import com.sgrecu.homeassignment.config.AppProperties
import com.sgrecu.homeassignment.config.TestConfig
import com.sgrecu.homeassignment.security.config.TestSecurityConfig
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.jwt.JwtTokenProvider
import com.sgrecu.homeassignment.security.model.UserPrincipal
import com.sgrecu.homeassignment.security.oauth.GoogleAuthProperties
import com.sgrecu.homeassignment.security.oauth.GoogleTokenVerifier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(
    TestSecurityConfig::class, AuthControllerIntegrationTest.TestMockBeansConfig::class, TestConfig::class
)
class AuthControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties::class, GoogleAuthProperties::class, AppProperties::class)
    internal class TestMockBeansConfig {
        // Mock dependencies
        val googleTokenVerifierMock = mockk<GoogleTokenVerifier>(relaxed = true)
        val jwtTokenProviderMock = mockk<JwtTokenProvider>(relaxed = true)

        @Bean
        fun googleTokenVerifier(): GoogleTokenVerifier {
            return googleTokenVerifierMock
        }

        @Bean
        fun jwtTokenProvider(): JwtTokenProvider {
            return jwtTokenProviderMock
        }

        @Bean
        fun flyway(): Flyway {
            return mockk<Flyway>(relaxed = true)
        }

        @Bean
        fun clock(): Clock {
            return Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.systemDefault())
        }
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var testMockBeansConfig: TestMockBeansConfig

    private val testGoogleToken = "valid.google.token"
    private val testJwtToken = "generated.jwt.token"
    private val testUserId = "google-user-123"
    private val testEmail = "test@example.com"

    @Test
    fun `authenticateWithGoogle should return JWT for valid token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)
        val userPrincipal = UserPrincipal(
            externalId = testUserId, email = testEmail, roles = setOf("USER")
        )

        every {
            testMockBeansConfig.googleTokenVerifierMock.verifyIdToken(testGoogleToken)
        } returns Mono.just(userPrincipal)

        every {
            testMockBeansConfig.jwtTokenProviderMock.generateToken(any())
        } returns testJwtToken

        // When/Then
        webTestClient.post().uri("/api/auth/google").contentType(MediaType.APPLICATION_JSON).bodyValue(tokenRequest)
            .exchange().expectStatus().isOk.expectBody().jsonPath("$.accessToken").isEqualTo(testJwtToken)
            .jsonPath("$.tokenType").isEqualTo("Bearer")

        verify {
            testMockBeansConfig.googleTokenVerifierMock.verifyIdToken(testGoogleToken)
        }
    }

    @Test
    fun `authenticateWithGoogle should return 400 for blank token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest("")

        // When/Then
        webTestClient.post().uri("/api/auth/google").contentType(MediaType.APPLICATION_JSON).bodyValue(tokenRequest)
            .exchange().expectStatus().isBadRequest
    }

    @Test
    fun `authenticateWithGoogle should return error for invalid token`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)

        every {
            testMockBeansConfig.googleTokenVerifierMock.verifyIdToken(testGoogleToken)
        } returns Mono.empty()

        // When/Then
        webTestClient.post().uri("/api/auth/google").contentType(MediaType.APPLICATION_JSON).bodyValue(tokenRequest)
            .exchange()
            .expectStatus().is5xxServerError // IllegalAccessException is not explicitly handled, so results in 500
    }

    @Test
    fun `authenticateWithGoogle should return 500 for verification error`() {
        // Given
        val tokenRequest = AuthController.GoogleTokenRequest(testGoogleToken)

        every {
            testMockBeansConfig.googleTokenVerifierMock.verifyIdToken(testGoogleToken)
        } returns Mono.error(RuntimeException("Token verification error"))

        // When/Then
        webTestClient.post().uri("/api/auth/google").contentType(MediaType.APPLICATION_JSON).bodyValue(tokenRequest)
            .exchange().expectStatus().is5xxServerError
    }
} 