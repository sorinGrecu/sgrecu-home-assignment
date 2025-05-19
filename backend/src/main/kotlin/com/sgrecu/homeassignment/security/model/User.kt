package com.sgrecu.homeassignment.security.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * User entity representing an authenticated user in the system.
 */
@Table("users")
data class User(
    @Id val id: Long? = null,

    @Column val externalId: String,

    @Column val email: String,

    @Column val displayName: String?,

    @Column val provider: String,

    @Column val createdAt: Instant,

    @Column val updatedAt: Instant
) 