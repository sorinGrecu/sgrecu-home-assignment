package com.sgrecu.homeassignment.security.config

import com.sgrecu.homeassignment.security.jwt.JwtAuthenticationConverter
import com.sgrecu.homeassignment.security.jwt.JwtAuthenticationFilter
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.jwt.JwtTokenProvider
import com.sgrecu.homeassignment.security.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Clock

/**
 * Central security configuration for testing
 * This configuration provides a standardized SecurityWebFilterChain for tests
 * along with mock implementations of security-related components.
 * We focus only on user roles, not admin roles.
 */
@TestConfiguration
@EnableWebFluxSecurity
class TestSecurityConfig {

    @Bean
    @Primary
    fun securityWebFilterChain(
        http: ServerHttpSecurity, jwtFilter: JwtAuthenticationFilter
    ): SecurityWebFilterChain =
        http.csrf(ServerHttpSecurity.CsrfSpec::disable).cors(ServerHttpSecurity.CorsSpec::disable)
            .authorizeExchange { ex ->
                ex.pathMatchers("/api/auth/**", "/api/test/public/**").permitAll().pathMatchers("/api/secured/**")
                    .hasRole("USER").anyExchange().authenticated()
            }.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION).exceptionHandling { exHandling ->
                exHandling.authenticationEntryPoint { exchange, _ -> handleUnauthorized(exchange) }
                    .accessDeniedHandler { exchange, _ -> handleForbidden(exchange) }
            }.build()

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

    @Bean
    @Profile("test")
    fun testClock(): Clock = Clock.systemUTC()

    @Bean
    @Primary
    @Profile("test")
    fun jwtTokenProvider(jwtProperties: JwtProperties): JwtTokenProvider = JwtTokenProvider(jwtProperties)

    @Bean
    @Primary
    @ConditionalOnMissingBean
    fun jwtAuthenticationConverter(
        jwtTokenProvider: JwtTokenProvider, userService: UserService
    ): JwtAuthenticationConverter = JwtAuthenticationConverter(jwtTokenProvider, userService)

    @Bean
    @Primary
    @ConditionalOnMissingBean
    fun jwtAuthenticationFilter(
        converter: JwtAuthenticationConverter, jwtProperties: JwtProperties
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(converter, jwtProperties)
} 