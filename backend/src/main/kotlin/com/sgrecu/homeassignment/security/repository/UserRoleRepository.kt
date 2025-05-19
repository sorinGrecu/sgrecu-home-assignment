package com.sgrecu.homeassignment.security.repository

import com.sgrecu.homeassignment.security.model.UserRole
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

/**
 * Repository for managing [UserRole] entities in the database.
 */
@Repository
interface UserRoleRepository : ReactiveCrudRepository<UserRole, Long> {

    /**
     * Retrieves all roles associated with a specific user.
     *
     * @param userId The ID of the user
     * @return A [Flux] of user roles for the specified user
     */
    fun findByUserId(userId: Long): Flux<UserRole>

}