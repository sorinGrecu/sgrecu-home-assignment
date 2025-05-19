package com.sgrecu.homeassignment.security.config

import com.sgrecu.homeassignment.config.AppProperties
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.oauth.GoogleAuthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class that enables property classes for security.
 */
@Configuration
@EnableConfigurationProperties(
    JwtProperties::class, GoogleAuthProperties::class, AppProperties::class
)
class SecurityBeansConfig 