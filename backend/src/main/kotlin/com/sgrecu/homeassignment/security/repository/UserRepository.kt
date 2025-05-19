package com.sgrecu.homeassignment.security.repository

import com.sgrecu.homeassignment.security.model.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

/**
 * Repository for managing [User] entities in the database.
 */
@Repository
interface UserRepository : ReactiveCrudRepository<User, Long> {

    /**
     * Finds a user by their external ID and authentication provider.
     *
     * @param externalId The ID provided by the external authentication service
     * @param provider The authentication provider name
     * @return A [Mono] containing the user if found, or empty if not
     */
    fun findByExternalIdAndProvider(externalId: String, provider: String): Mono<User>

} 