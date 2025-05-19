package com.sgrecu.homeassignment.security.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Google OAuth authentication.
 */
@ConfigurationProperties(prefix = "app.security.oauth2.google")
data class GoogleAuthProperties(
    val clientId: String
) 