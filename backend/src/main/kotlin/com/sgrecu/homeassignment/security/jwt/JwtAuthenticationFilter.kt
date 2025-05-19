package com.sgrecu.homeassignment.security.jwt

import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * WebFilter responsible for JWT token-based authentication.
 * Intercepts requests, extracts JWT tokens, and authenticates users.
 * Skips authentication for OPTIONS requests (CORS preflight).
 */
@Component
class JwtAuthenticationFilter(
    @Lazy private val jwtAuthenticationConverter: JwtAuthenticationConverter, private val jwtProperties: JwtProperties
) : WebFilter {
    private val logger = KotlinLogging.logger {}

    /**
     * Filters requests to authenticate users based on JWT tokens.
     *
     * @param exchange The current server exchange
     * @param chain The web filter chain
     * @return Mono<Void> Completes when the filter chain is complete
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange.request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        val token = extractToken(exchange)

        if (token.isNullOrBlank()) {
            return chain.filter(exchange)
        }

        return jwtAuthenticationConverter.convert(token).flatMap { auth ->
                chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            }.onErrorResume { error ->
                logger.warn(
                    "JWT authentication failed: path={}, method={}, remote={}, error={}",
                    exchange.request.path.value(),
                    exchange.request.method,
                    exchange.request.remoteAddress?.address?.hostAddress ?: "unknown",
                    error.message
                )

                val message = when (error) {
                    is UsernameNotFoundException -> "User not found"
                    else -> "Authentication failed"
                }

                Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED, message))
            }
    }

    /**
     * Extracts JWT token from the request.
     * First attempts to extract from Authorization header.
     * If allowed by configuration, also attempts to extract from query parameters.
     *
     * @param exchange The current server exchange
     * @return The extracted token, or null if none found
     */
    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            return authHeader.removePrefix("Bearer ").takeIf { it.isNotBlank() }
        }

        if (jwtProperties.allowTokenQueryParameter) {
            return exchange.request.queryParams.getFirst("token")
        }

        return null
    }
}
