package com.sgrecu.homeassignment.security.jwt

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration

/**
 * Tests for JwtAuthenticationFilter focusing on the filter() method
 * and its various behaviors under different conditions.
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var exchange: ServerWebExchange

    @Mock
    private lateinit var request: ServerHttpRequest

    @Mock
    private lateinit var response: ServerHttpResponse

    @Mock
    private lateinit var chain: WebFilterChain

    @Mock
    private lateinit var jwtAuthenticationConverter: JwtAuthenticationConverter

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private lateinit var jwtProperties: JwtProperties

    private val validToken = "valid.jwt.token"

    @BeforeEach
    fun setup() {
        jwtProperties = JwtProperties(
            secretKey = "test-secret-key-that-is-long-enough-for-hmac-sha256",
            expiration = 3600000,
            issuer = "test-issuer",
            audience = "test-audience",
            allowTokenQueryParameter = false
        )

        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtAuthenticationConverter, jwtProperties)

        // Common setup for the ServerWebExchange mock
        `when`(exchange.request).thenReturn(request)
        `when`(exchange.response).thenReturn(response)
        `when`(request.uri).thenReturn(URI.create("http://localhost:8080/api/test"))

        val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()
        `when`(request.queryParams).thenReturn(queryParams)

        val headers = HttpHeaders()
        `when`(request.headers).thenReturn(headers)

        val mockAddress = InetSocketAddress(InetAddress.getLocalHost(), 12345)
        `when`(request.remoteAddress).thenReturn(mockAddress)

        `when`(chain.filter(exchange)).thenReturn(Mono.empty())
    }

    @Nested
    @DisplayName("filter() method with preprocessing request tests")
    inner class FilterPreprocessingTests {

        @Test
        @DisplayName("Should bypass authentication for OPTIONS requests")
        fun filter_withOptionsRequest_bypassesAuthentication() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.OPTIONS)

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter, never()).convert(anyString())
            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("Should continue filter chain when no token is present")
        fun filter_withNoToken_continuesFilterChain() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter, never()).convert(anyString())
            verify(chain).filter(exchange)
        }
    }

    @Nested
    @DisplayName("filter() method with token extraction tests")
    inner class FilterTokenExtractionTests {

        @Test
        @DisplayName("Should extract token from Authorization header")
        fun filter_withAuthorizationHeader_extractsToken() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            `when`(request.headers).thenReturn(headers)

            val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))
            val mockAuth = UsernamePasswordAuthenticationToken("user", null, authorities)

            `when`(jwtAuthenticationConverter.convert(validToken)).thenReturn(Mono.just(mockAuth))

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter).convert(validToken)
        }

        @Test
        @DisplayName("Should not extract token from query parameter when disabled")
        fun filter_withQueryParameter_doesNotExtractTokenWhenDisabled() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)

            val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()
            queryParams.add("token", validToken)
            `when`(request.queryParams).thenReturn(queryParams)

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter, never()).convert(anyString())
            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("Should extract token from query parameter when enabled")
        fun filter_withQueryParameter_extractsTokenWhenEnabled() {
            // Given
            jwtProperties = JwtProperties(
                secretKey = "test-secret-key-that-is-long-enough-for-hmac-sha256",
                expiration = 3600000,
                issuer = "test-issuer",
                audience = "test-audience",
                allowTokenQueryParameter = true
            )
            jwtAuthenticationFilter = JwtAuthenticationFilter(jwtAuthenticationConverter, jwtProperties)

            `when`(request.method).thenReturn(HttpMethod.GET)

            val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()
            queryParams.add("token", validToken)
            `when`(request.queryParams).thenReturn(queryParams)

            val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))
            val mockAuth = UsernamePasswordAuthenticationToken("user", null, authorities)

            `when`(jwtAuthenticationConverter.convert(validToken)).thenReturn(Mono.just(mockAuth))

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter).convert(validToken)
        }

        @Test
        @DisplayName("Should ignore auth header without Bearer prefix")
        fun filter_withInvalidAuthHeader_skipsAuthentication() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==")
            `when`(request.headers).thenReturn(headers)

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter, never()).convert(anyString())
            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("Should ignore empty token in Bearer header")
        fun filter_withEmptyToken_skipsAuthentication() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer ")
            `when`(request.headers).thenReturn(headers)

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter, never()).convert(anyString())
            verify(chain).filter(exchange)
        }
    }

    @Nested
    @DisplayName("filter() method with authentication error handling tests")
    inner class FilterAuthenticationErrorHandlingTests {

        @Test
        @DisplayName("Should handle authentication error with 401 status")
        fun filter_withInvalidToken_failsAuthentication() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            `when`(request.headers).thenReturn(headers)

            `when`(jwtAuthenticationConverter.convert(anyString())).thenReturn(Mono.error(RuntimeException("Auth failed")))

            // When & Then
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            var caughtException = false
            try {
                result.block(Duration.ofSeconds(1))
            } catch (ex: Exception) {
                caughtException = true
            }

            assert(caughtException) { "Expected an exception to be thrown, but none was caught" }
            verify(jwtAuthenticationConverter).convert(anyString())
            verify(chain, never()).filter(any()) // Chain should not be called when auth fails
        }

        @Test
        @DisplayName("Should handle UsernameNotFoundException with specific message")
        fun filter_withUserNotFound_returnsUserNotFoundMessage() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            `when`(request.headers).thenReturn(headers)

            val requestPath = mock(org.springframework.http.server.RequestPath::class.java)
            `when`(requestPath.value()).thenReturn("/api/endpoint")
            `when`(request.path).thenReturn(requestPath)
            `when`(jwtAuthenticationConverter.convert(anyString())).thenReturn(
                Mono.error(UsernameNotFoundException("User not found in database"))
            )

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).expectErrorMatches { error ->
                error is ResponseStatusException && error.statusCode == HttpStatus.UNAUTHORIZED && error.reason == "User not found"
            }.verify()
            verify(jwtAuthenticationConverter).convert(anyString())
            verify(chain, never()).filter(any())
        }

        @Test
        @DisplayName("Should handle null remoteAddress gracefully")
        fun filter_withNullRemoteAddress_logsUnknownAddress() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            `when`(request.headers).thenReturn(headers)

            `when`(request.remoteAddress).thenReturn(null)

            val requestPath = mock(org.springframework.http.server.RequestPath::class.java)
            `when`(requestPath.value()).thenReturn("/api/endpoint")
            `when`(request.path).thenReturn(requestPath)
            `when`(jwtAuthenticationConverter.convert(anyString())).thenReturn(
                Mono.error(RuntimeException("Authentication error"))
            )

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).expectError(ResponseStatusException::class.java).verify()
            verify(jwtAuthenticationConverter).convert(anyString())
            verify(chain, never()).filter(any())
        }
    }

    @Nested
    @DisplayName("filter() method with successful authentication tests")
    inner class FilterAuthenticationSuccessTests {

        @Test
        @DisplayName("Should authenticate successfully with valid token")
        fun filter_withValidToken_authenticatesSuccessfully() {
            // Given
            `when`(request.method).thenReturn(HttpMethod.GET)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            `when`(request.headers).thenReturn(headers)

            val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))
            val mockAuth = UsernamePasswordAuthenticationToken("user", null, authorities)

            `when`(jwtAuthenticationConverter.convert(validToken)).thenReturn(Mono.just(mockAuth))

            // When
            val result = jwtAuthenticationFilter.filter(exchange, chain)

            // Then
            StepVerifier.create(result).verifyComplete()
            verify(jwtAuthenticationConverter).convert(validToken)
            verify(chain).filter(any())
        }
    }
} 