package com.sgrecu.homeassignment.security.config

import com.sgrecu.homeassignment.security.jwt.JwtAuthenticationFilter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Configuration properties for CORS settings.
 * Controls the cross-origin resource sharing behavior of the application.
 */
@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:3000"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val exposedHeaders: List<String> = listOf("Authorization", "Content-Type"),
    val maxAge: Long = 3600,
    val allowCredentials: Boolean = true
)

/**
 * Security configuration for the application.
 * Configures JWT authentication, CORS, and request authorization rules.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter, private val corsProperties: CorsProperties
) {
    /**
     * Configures the security filter chain for the application.
     * Sets up authentication, authorization, CORS, and exception handling.
     *
     * @param http The server HTTP security configuration
     * @return The configured security web filter chain
     */
    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchanges ->
                exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll().pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers("/api/test/public/**").permitAll().pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            }.cors { it.configurationSource(corsConfigurationSource()) }.csrf { it.disable() }
            .httpBasic { it.disable() }.formLogin { it.disable() }.exceptionHandling { exHandling ->
                exHandling.authenticationEntryPoint { exchange, _ -> handleUnauthorized(exchange) }
                    .accessDeniedHandler { exchange, _ -> handleForbidden(exchange) }
            }.addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION).build()
    }

    /**
     * Handles unauthorized access attempts by returning a 401 response.
     *
     * @param exchange The current server web exchange
     * @return Mono completing when the response has been written
     */
    private fun handleUnauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        return response.writeWith(
            Mono.just(
                response.bufferFactory().wrap(
                    """{"status":401,"message":"Unauthorized"}""".toByteArray()
                )
            )
        )
    }

    /**
     * Handles forbidden access attempts by returning a 403 response.
     *
     * @param exchange The current server web exchange
     * @return Mono completing when the response has been written
     */
    private fun handleForbidden(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.contentType = MediaType.APPLICATION_JSON
        return response.writeWith(
            Mono.just(
                response.bufferFactory().wrap(
                    """{"status":403,"message":"Forbidden"}""".toByteArray()
                )
            )
        )
    }

    /**
     * Creates and configures the CORS configuration source.
     * Uses settings from CorsProperties to define allowed origins, methods, headers, etc.
     *
     * @return The configured CORS configuration source
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource = UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration("/**", CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            maxAge = corsProperties.maxAge
            allowedMethods = corsProperties.allowedMethods
            allowedHeaders = corsProperties.allowedHeaders
            exposedHeaders = corsProperties.exposedHeaders
            allowCredentials = corsProperties.allowCredentials
        })
    }
} 