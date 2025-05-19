package com.sgrecu.homeassignment.security.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * A user principal for Google (or any OIDC/OAuth2) logins.
 * Implements standard Spring Security interfaces.
 */
data class UserPrincipal(

    val id: String? = null,

    /**
     * The user's Google (or other OAuth) ID.
     */
    val externalId: String,

    /**
     * Email as provided by the OAuth provider.
     */
    val email: String,

    /**
     * High-level roles, e.g., ADMIN or USER.
     */
    val roles: Set<String> = emptySet(),

    /**
     * Raw OAuth2 attributes (profile picture, locale, etc.).
     */
    private val attributes: Map<String, Any> = emptyMap()
) : UserDetails, OAuth2User {

    /**
     * Convert roles to Spring Security authorities.
     * Spring Security typically distinguishes roles with a "ROLE_" prefix.
     *
     * Note: This can be extended in the future to include fine-grained permissions.
     */
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return roles.map { SimpleGrantedAuthority("ROLE_$it") }
    }

    override fun getAttributes(): Map<String, Any> = attributes
    override fun getName(): String = externalId
    override fun getUsername(): String = email
    override fun getPassword(): String? = null
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
    fun getUserId(): String = externalId
} 