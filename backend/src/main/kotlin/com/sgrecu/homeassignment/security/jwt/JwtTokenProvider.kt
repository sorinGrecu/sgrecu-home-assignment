package com.sgrecu.homeassignment.security.jwt

import com.sgrecu.homeassignment.security.model.UserPrincipal
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*
import javax.crypto.SecretKey

/**
 * Service responsible for JWT token generation, parsing, and validation.
 */
@Service
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray())
    }

    /**
     * Generates a JWT token based on the authentication information.
     * 
     * @param authentication The authentication object containing user details
     * @return JWT token as a string
     */
    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.expiration)

        return Jwts.builder().setSubject(userPrincipal.getUserId()).claim("email", userPrincipal.username)
            .claim("roles", userPrincipal.roles).setIssuedAt(now).setExpiration(expiryDate)
            .setIssuer(jwtProperties.issuer).setAudience(jwtProperties.audience)
            .signWith(secretKey, SignatureAlgorithm.HS256).compact()
    }

    /**
     * Validates a JWT token.
     * 
     * @param token The JWT token to validate
     * @return Mono containing true if token is valid, false otherwise
     */
    fun validateToken(token: String): Mono<Boolean> {
        return parseToken(token).map { true }.onErrorReturn(false)
    }

    /**
     * Extracts the user ID from a JWT token.
     * 
     * @param token The JWT token
     * @return Mono containing the user ID or empty string if extraction fails
     */
    fun getUserIdFromToken(token: String): Mono<String> {
        return parseToken(token).map { it.subject ?: "" }.onErrorReturn("")
    }

    /**
     * Parses a JWT token to extract its claims.
     * 
     * @param token The JWT token to parse
     * @return Mono containing the claims of the token
     */
    private fun parseToken(token: String): Mono<Claims> {
        return Mono.fromCallable {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body
        }.subscribeOn(Schedulers.boundedElastic())
    }
} 