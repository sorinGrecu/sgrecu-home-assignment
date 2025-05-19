package com.sgrecu.homeassignment.security.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

/**
 * Entity representing a role assigned to a user.
 */
@Table("user_roles")
data class UserRole(
    @Id val id: Long? = null,

    @Column val userId: Long,

    @Column val role: String
) 