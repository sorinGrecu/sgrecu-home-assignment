package com.sgrecu.homeassignment.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for JWT token generation and validation.
 * These properties are loaded from application configuration with the prefix "app.security.jwt".
 */
@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    /**
     * The secret key used for signing and validating JWT tokens.
     */
    val secretKey: String,
    
    /**
     * Token expiration time in milliseconds.
     * Defines how long the generated token will be valid.
     */
    val expiration: Long,
    
    /**
     * The issuer claim for the JWT token.
     * Identifies the principal that issued the token.
     */
    val issuer: String,
    
    /**
     * The audience claim for the JWT token.
     * Identifies the recipients that the JWT is intended for.
     */
    val audience: String,
    
    /**
     * Flag to allow token extraction from query parameters.
     * When true, the application will attempt to extract tokens from the "token" query parameter.
     * Default is false for security reasons.
     */
    val allowTokenQueryParameter: Boolean = false
) 