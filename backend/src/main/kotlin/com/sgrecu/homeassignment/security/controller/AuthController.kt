package com.sgrecu.homeassignment.security.controller

import com.sgrecu.homeassignment.security.jwt.JwtTokenProvider
import com.sgrecu.homeassignment.security.oauth.GoogleTokenVerifier
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Authentication controller for Google Sign-In that returns a JWT.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val googleTokenVerifier: GoogleTokenVerifier, private val jwtTokenProvider: JwtTokenProvider
) {
    private val logger = KotlinLogging.logger {}

    data class GoogleTokenRequest(val idToken: String)

    data class JwtResponse(val accessToken: String, val tokenType: String = "Bearer")

    @PostMapping("/google")
    fun authenticateWithGoogle(@RequestBody tokenRequest: GoogleTokenRequest): Mono<JwtResponse> {
        if (tokenRequest.idToken.isBlank()) {
            return Mono.error(IllegalArgumentException("ID token cannot be empty"))
        }

        return googleTokenVerifier.verifyIdToken(tokenRequest.idToken).switchIfEmpty(
            Mono.error(IllegalAccessException("Invalid or expired Google ID token"))
        ).map { userPrincipal ->
            UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.authorities
            )
        }.map { authentication ->
            val jwt = jwtTokenProvider.generateToken(authentication)
            JwtResponse(accessToken = jwt)
        }.doOnError { e ->
            logger.error { "Failed to authenticate via Google token: ${e.message}" }
        }
    }
}