package com.sgrecu.homeassignment.security.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Minimal security configuration for WebFluxTest slices
 * Deliberately permits all auth endpoints to avoid security filter chain interference
 * during controller-focused tests.
 */
@TestConfiguration
class SecurityTestConfiguration {

    @Bean
    fun testSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange { ex ->
                ex.pathMatchers("/api/auth/**").permitAll()
                    .anyExchange().denyAll()
            }
            .build()
} 