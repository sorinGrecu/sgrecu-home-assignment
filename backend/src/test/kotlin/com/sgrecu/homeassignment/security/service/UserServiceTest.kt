package com.sgrecu.homeassignment.security.service

import com.sgrecu.homeassignment.security.model.RoleEnum
import com.sgrecu.homeassignment.security.model.User
import com.sgrecu.homeassignment.security.model.UserRole
import com.sgrecu.homeassignment.security.repository.UserRepository
import com.sgrecu.homeassignment.security.repository.UserRoleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Tests for the UserService class
 */
@ExtendWith(MockitoExtension::class, SpringExtension::class)
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userRoleRepository: UserRoleRepository
    private lateinit var clock: Clock
    private lateinit var userService: UserService

    private val testExternalId = "test-ext-id"
    private val testEmail = "test@example.com"
    private val testDisplayName = "Test User"
    private val testProvider = "google"
    private val testNow = Instant.parse("2023-01-01T12:00:00Z")

    private lateinit var testUser: User
    
    @BeforeEach
    fun setup() {
        userRepository = Mockito.mock(UserRepository::class.java)
        userRoleRepository = Mockito.mock(UserRoleRepository::class.java)
        clock = Clock.fixed(testNow, ZoneId.systemDefault())
        userService = UserService(userRepository, userRoleRepository, clock)
        
        testUser = User(
            id = 1L,
            externalId = testExternalId,
            email = testEmail,
            displayName = testDisplayName,
            provider = testProvider,
            createdAt = testNow,
            updatedAt = testNow
        )
        
        // Default behavior for count method
        Mockito.lenient().`when`(userRepository.count()).thenReturn(Mono.just(0L))
    }

    @Test
    fun `findOrCreateUser should return existing user when found`() {
        // Given
        val userRoles = listOf(UserRole(userId = 1L, role = RoleEnum.USER.name))
        
        Mockito.`when`(userRepository.findByExternalIdAndProvider(testExternalId, testProvider))
            .thenReturn(Mono.just(testUser))
        Mockito.`when`(userRoleRepository.findByUserId(testUser.id!!))
            .thenReturn(Flux.fromIterable(userRoles))

        // When & Then
        StepVerifier.create(
            userService.findOrCreateUser(
                testExternalId,
                testEmail,
                testDisplayName,
                testProvider
            )
        )
            .expectNextMatches { userWithRoles ->
                userWithRoles.user.id == testUser.id &&
                userWithRoles.user.externalId == testExternalId &&
                userWithRoles.user.email == testEmail &&
                userWithRoles.roles.size == 1 &&
                userWithRoles.roles.contains(RoleEnum.USER.name)
            }
            .verifyComplete()
    }

    @Test
    fun `findOrCreateUser should create new user with USER role when user does not exist and not first user`() {
        // Given
        val savedUser = User(
            id = 1L,
            externalId = testExternalId,
            email = testEmail,
            displayName = testDisplayName,
            provider = testProvider,
            createdAt = testNow,
            updatedAt = testNow
        )
        val savedUserRole = UserRole(userId = 1L, role = RoleEnum.USER.name)

        Mockito.`when`(userRepository.findByExternalIdAndProvider(testExternalId, testProvider))
            .thenReturn(Mono.empty())
        Mockito.`when`(userRepository.count()).thenReturn(Mono.just(1L)) // Not first user
        Mockito.`when`(userRepository.save(Mockito.any(User::class.java))).thenReturn(Mono.just(savedUser))
        Mockito.`when`(userRoleRepository.save(Mockito.any(UserRole::class.java))).thenReturn(Mono.just(savedUserRole))
        Mockito.`when`(userRoleRepository.findByUserId(1L))
            .thenReturn(Flux.just(savedUserRole))

        // When & Then
        StepVerifier.create(
            userService.findOrCreateUser(
                testExternalId,
                testEmail,
                testDisplayName,
                testProvider
            )
        )
            .expectNextMatches { userWithRoles ->
                userWithRoles.user.id == savedUser.id &&
                userWithRoles.user.externalId == testExternalId &&
                userWithRoles.user.email == testEmail &&
                userWithRoles.roles.size == 1 &&
                userWithRoles.roles.contains(RoleEnum.USER.name)
            }
            .verifyComplete()
    }

    @Test
    fun `findOrCreateUser should create new user with OWNER role when user is the first in system`() {
        // Given
        val savedUser = User(
            id = 1L,
            externalId = testExternalId,
            email = testEmail,
            displayName = testDisplayName,
            provider = testProvider,
            createdAt = testNow,
            updatedAt = testNow
        )
        val ownerRole = UserRole(userId = 1L, role = RoleEnum.OWNER.name)
        val adminRole = UserRole(userId = 1L, role = RoleEnum.ADMIN.name)
        val userRole = UserRole(userId = 1L, role = RoleEnum.USER.name)

        Mockito.`when`(userRepository.findByExternalIdAndProvider(testExternalId, testProvider))
            .thenReturn(Mono.empty())
        Mockito.`when`(userRepository.count()).thenReturn(Mono.just(0L)) // First user
        Mockito.`when`(userRepository.save(Mockito.any(User::class.java))).thenReturn(Mono.just(savedUser))
        
        Mockito.`when`(userRoleRepository.save(Mockito.any(UserRole::class.java)))
            .thenReturn(Mono.just(ownerRole))
            .thenReturn(Mono.just(adminRole))
            .thenReturn(Mono.just(userRole))
            
        Mockito.`when`(userRoleRepository.findByUserId(1L))
            .thenReturn(Flux.just(ownerRole, adminRole, userRole))

        // When & Then
        StepVerifier.create(
            userService.findOrCreateUser(
                testExternalId,
                testEmail,
                testDisplayName,
                testProvider
            )
        )
            .expectNextMatches { userWithRoles ->
                userWithRoles.user.id == savedUser.id &&
                userWithRoles.user.externalId == testExternalId &&
                userWithRoles.user.email == testEmail &&
                userWithRoles.roles.size == 3 &&
                userWithRoles.roles.contains(RoleEnum.OWNER.name) &&
                userWithRoles.roles.contains(RoleEnum.ADMIN.name) &&
                userWithRoles.roles.contains(RoleEnum.USER.name)
            }
            .verifyComplete()
    }

    @Test
    fun `getUserByExternalId should return user with roles when found`() {
        // Given
        val userRoles = listOf(UserRole(userId = 1L, role = RoleEnum.USER.name))
        
        Mockito.`when`(userRepository.findByExternalIdAndProvider(testExternalId, testProvider))
            .thenReturn(Mono.just(testUser))
        Mockito.`when`(userRoleRepository.findByUserId(testUser.id!!))
            .thenReturn(Flux.fromIterable(userRoles))

        // When & Then
        StepVerifier.create(
            userService.getUserByExternalId(testExternalId, testProvider)
        )
            .expectNextMatches { userWithRoles ->
                userWithRoles.user.id == testUser.id &&
                userWithRoles.user.externalId == testExternalId &&
                userWithRoles.roles.size == 1 &&
                userWithRoles.roles.contains(RoleEnum.USER.name)
            }
            .verifyComplete()
    }

    @Test
    fun `getUserByExternalId should return empty when user not found`() {
        // Given
        Mockito.`when`(userRepository.findByExternalIdAndProvider(testExternalId, testProvider))
            .thenReturn(Mono.empty())

        // When & Then
        StepVerifier.create(
            userService.getUserByExternalId(testExternalId, testProvider)
        )
            .verifyComplete()
    }
} 