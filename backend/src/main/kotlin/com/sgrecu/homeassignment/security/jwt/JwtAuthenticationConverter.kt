package com.sgrecu.homeassignment.security.jwt

import com.sgrecu.homeassignment.security.model.UserPrincipal
import com.sgrecu.homeassignment.security.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Converts JWT tokens to Spring Security authentication objects.
 * Validates the token, extracts user information, and creates appropriate authentication tokens.
 */
@Component
class JwtAuthenticationConverter(
    private val jwtTokenProvider: JwtTokenProvider, private val userService: UserService
) {

    /**
     * Converts a JWT token to a UsernamePasswordAuthenticationToken.
     * Validates the token and loads user details from the database.
     *
     * @param token The JWT token to convert
     * @return Mono containing the authentication token or empty if token is invalid
     */
    fun convert(token: String): Mono<UsernamePasswordAuthenticationToken> {
        return jwtTokenProvider.validateToken(token).flatMap { isValid ->
                if (!isValid) {
                    return@flatMap Mono.empty()
                }

                processValidToken(token)
            }
    }

    /**
     * Processes a validated token to create an authentication token.
     *
     * @param token The validated JWT token
     * @return Mono containing the authentication token or empty if processing fails
     */
    private fun processValidToken(token: String): Mono<UsernamePasswordAuthenticationToken> {
        return jwtTokenProvider.getUserIdFromToken(token).flatMap { userId ->
                if (userId.isBlank()) {
                    return@flatMap Mono.empty()
                }

                createAuthenticationToken(userId, token)
            }
    }

    /**
     * Creates an authentication token for a given user ID.
     * Loads user details from the database and builds the authentication object.
     *
     * @param userId The user ID extracted from the token
     * @param token The original JWT token used as credentials
     * @return Mono containing the authentication token
     * @throws UsernameNotFoundException If user is not found in the database
     */
    private fun createAuthenticationToken(userId: String, token: String): Mono<UsernamePasswordAuthenticationToken> {
        return userService.getUserByExternalId(userId, "google")
            .switchIfEmpty(Mono.error(UsernameNotFoundException("User not found in the database")))
            .map { userWithRoles ->
                val authorities = userWithRoles.roles.map(::SimpleGrantedAuthority).toSet()

                val principal = UserPrincipal(
                    externalId = userWithRoles.user.externalId,
                    email = userWithRoles.user.email,
                    roles = userWithRoles.roles
                )

                UsernamePasswordAuthenticationToken(principal, token, authorities)
            }
    }
}