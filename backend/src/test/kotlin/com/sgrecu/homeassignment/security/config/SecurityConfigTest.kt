package com.sgrecu.homeassignment.security.config

import com.sgrecu.homeassignment.security.jwt.JwtAuthenticationFilter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityConfigTest {

    private val jwtAuthenticationFilter = mock<JwtAuthenticationFilter>()
    private val defaultCorsProperties = CorsProperties()
    private val securityConfig = SecurityConfig(jwtAuthenticationFilter, defaultCorsProperties)

    @Test
    fun `corsConfigurationSource should allow configured origins`() {
        // Given
        val corsProperties = CorsProperties(
            allowedOrigins = listOf("https://example.com"),
            allowedMethods = listOf("GET", "POST"),
            allowedHeaders = listOf("X-Custom-Header"),
            exposedHeaders = listOf("X-Exposed-Header"),
            maxAge = 7200,
            allowCredentials = true
        )
        val securityConfig = SecurityConfig(jwtAuthenticationFilter, corsProperties)
        
        // When
        val corsConfigSource = securityConfig.corsConfigurationSource()
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        )
        val config = corsConfigSource.getCorsConfiguration(exchange)
        
        // Then
        assertNotNull(config)
        assertEquals(listOf("https://example.com"), config.allowedOrigins)
        assertEquals(listOf("GET", "POST"), config.allowedMethods)
        assertEquals(listOf("X-Custom-Header"), config.allowedHeaders)
        assertEquals(listOf("X-Exposed-Header"), config.exposedHeaders)
        assertEquals(7200, config.maxAge)
        assertEquals(true, config.allowCredentials)
    }
    
    @Test
    fun `handleUnauthorized should return 401 response`() {
        // Given
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        )
        
        // When
        val result = securityConfig::class.java.getDeclaredMethod("handleUnauthorized", ServerWebExchange::class.java)
            .apply { isAccessible = true }
            .invoke(securityConfig, exchange) as Mono<Void>
            
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, exchange.response.headers.contentType)
    }
    
    @Test
    fun `handleForbidden should return 403 response`() {
        // Given
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        )
        
        // When
        val result = securityConfig::class.java.getDeclaredMethod("handleForbidden", ServerWebExchange::class.java)
            .apply { isAccessible = true }
            .invoke(securityConfig, exchange) as Mono<Void>
            
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        assertEquals(HttpStatus.FORBIDDEN, exchange.response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, exchange.response.headers.contentType)
    }
    
    @Test
    fun `cors configuration should use default values if not specified`() {
        // When
        val corsConfigSource = securityConfig.corsConfigurationSource()
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        )
        val config = corsConfigSource.getCorsConfiguration(exchange)
        
        // Then
        assertNotNull(config)
        assertEquals(listOf("http://localhost:3000"), config.allowedOrigins)
        assertTrue(config.allowedMethods?.containsAll(listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")) ?: false)
    }
} 