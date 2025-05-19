package com.sgrecu.homeassignment.security.service

import com.sgrecu.homeassignment.security.model.RoleEnum
import com.sgrecu.homeassignment.security.model.User
import com.sgrecu.homeassignment.security.model.UserRole
import com.sgrecu.homeassignment.security.repository.UserRepository
import com.sgrecu.homeassignment.security.repository.UserRoleRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant

/**
 * Service for managing user authentication, creation, and role assignment.
 * Handles user lookup, creation, and role management for the application security system.
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val clock: Clock
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Provides current time using the injected clock.
     * Uses dependency-injected clock to facilitate testing.
     */
    private fun now() = Instant.now(clock)

    /**
     * Finds an existing user or creates a new one based on external authentication.
     * If this is the first user in the system, they are automatically assigned OWNER role.
     *
     * @param externalId Unique identifier from the authentication provider
     * @param email User's email address
     * @param displayName User's display name (optional)
     * @param provider Authentication provider identifier
     * @return Mono containing the user with their assigned roles
     */
    @Transactional
    fun findOrCreateUser(
        externalId: String, email: String, displayName: String?, provider: String
    ): Mono<UserWithRoles> {
        return userRepository.findByExternalIdAndProvider(externalId, provider).switchIfEmpty(
            userRepository.count().flatMap { count ->
                val isFirstUser = count == 0L
                val now = now()
                val newUser = User(
                    externalId = externalId,
                    email = email,
                    displayName = displayName,
                    provider = provider,
                    createdAt = now,
                    updatedAt = now
                )

                userRepository.save(newUser).flatMap { savedUser ->
                    val rolesToAssign = if (isFirstUser) {
                        logger.info { "First user registered! User ${savedUser.email} (ID: ${savedUser.externalId}) has been granted OWNER role" }
                        listOf(RoleEnum.OWNER.name, RoleEnum.ADMIN.name, RoleEnum.USER.name)
                    } else {
                        listOf(RoleEnum.USER.name)
                    }

                    val savedRolesMono = Flux.fromIterable(rolesToAssign).flatMap { role ->
                        userRoleRepository.save(UserRole(userId = savedUser.id!!, role = role))
                    }.collectList()

                    savedRolesMono.thenReturn(savedUser)
                }
            }).flatMap { user ->
            getUserRoles(user)
        }
    }

    /**
     * Retrieves a user with their roles by external ID and provider.
     *
     * @param externalId Unique identifier from the authentication provider
     * @param provider Authentication provider identifier
     * @return Mono containing the user with their assigned roles, or empty if not found
     */
    fun getUserByExternalId(externalId: String, provider: String): Mono<UserWithRoles> {
        return userRepository.findByExternalIdAndProvider(externalId, provider).flatMap { user ->
            getUserRoles(user)
        }
    }

    /**
     * Fetches roles for a given user and combines them with the user object.
     *
     * @param user The user entity to fetch roles for
     * @return Mono containing the user with their assigned roles
     */
    private fun getUserRoles(user: User): Mono<UserWithRoles> {
        return userRoleRepository.findByUserId(user.id!!).map { it.role }.collectList().map { roles ->
            UserWithRoles(
                user = user, roles = roles.toSet()
            )
        }
    }

    /**
     * Data structure that combines a user with their assigned roles.
     * Used for authentication and authorization decisions.
     *
     * @property user The user entity
     * @property roles Set of role names assigned to the user
     */
    data class UserWithRoles(
        val user: User, val roles: Set<String>
    )
}