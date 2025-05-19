package com.sgrecu.homeassignment.security.jwt

import com.sgrecu.homeassignment.security.model.UserPrincipal
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private lateinit var jwtProperties: JwtProperties
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setup() {
        jwtProperties = JwtProperties(
            secretKey = "thisissecretthatismorethan256bits-thisissecretthatismorethan256bits",
            expiration = 3600000, // 1 hour
            issuer = "sgrecu-home-assignment-backend-test",
            audience = "test-audience"
        )

        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Test
    fun `generateToken and getUserIdFromToken should work in happy path`() {
        // Given
        val userId = "user-123"
        val email = "user@example.com"
        val roles = setOf("USER")

        val userPrincipal = UserPrincipal(
            externalId = userId, email = email, roles = roles
        )
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)

        // When
        val token = jwtTokenProvider.generateToken(authentication)

        // Then
        StepVerifier.create(jwtTokenProvider.getUserIdFromToken(token)).expectNext(userId).verifyComplete()

        StepVerifier.create(jwtTokenProvider.validateToken(token)).expectNext(true).verifyComplete()
    }

    @Test
    fun `validateToken returns false for malformed token`() {
        // Given
        val malformedToken = "not.a.valid.jwt.token"

        // When/Then
        StepVerifier.create(jwtTokenProvider.validateToken(malformedToken)).expectNext(false).verifyComplete()
    }

    @Test
    fun `validateToken returns false for expired token`() {
        // Given
        // Create a properties with very short expiration
        val expiredProperties = JwtProperties(
            secretKey = jwtProperties.secretKey, expiration = 1, // 1 ms
            issuer = jwtProperties.issuer, audience = jwtProperties.audience
        )

        val expiredProvider = JwtTokenProvider(expiredProperties)

        val userId = "user-123"
        val email = "user@example.com"
        val roles = setOf("USER")

        val userPrincipal = UserPrincipal(
            externalId = userId, email = email, roles = roles
        )
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)

        // When
        val token = expiredProvider.generateToken(authentication)

        // A small delay to ensure the token is expired
        Thread.sleep(10)

        // Then
        StepVerifier.create(expiredProvider.validateToken(token)).expectNext(false).verifyComplete()
    }

    @Test
    fun `validateToken returns false for token with incorrect signature`() {
        // Given
        val userId = "user-123"
        val email = "user@example.com"
        val roles = setOf("USER")

        val userPrincipal = UserPrincipal(
            externalId = userId, email = email, roles = roles
        )
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)

        // Create a token with the original provider
        val token = jwtTokenProvider.generateToken(authentication)

        // Create a provider with a different secret key
        val differentProperties = JwtProperties(
            secretKey = "differentsecrethereismorethan256bits-differentsecrethereismorethan256bits",
            expiration = jwtProperties.expiration,
            issuer = jwtProperties.issuer,
            audience = jwtProperties.audience
        )

        val differentProvider = JwtTokenProvider(differentProperties)

        // Then
        StepVerifier.create(differentProvider.validateToken(token)).expectNext(false).verifyComplete()
    }

    @Test
    fun `getUserIdFromToken returns empty for invalid token`() {
        // Given
        val invalidToken = "invalid.token"

        // When/Then
        StepVerifier.create(jwtTokenProvider.getUserIdFromToken(invalidToken)).expectNext("").verifyComplete()
    }

    @Test
    fun `generateToken includes all expected claims`() {
        // Given
        val userId = "user-123"
        val email = "user@example.com"
        val roles = setOf("USER", "ADMIN")

        val userPrincipal = UserPrincipal(
            externalId = userId, email = email, roles = roles
        )
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)

        // When
        val token = jwtTokenProvider.generateToken(authentication)

        // Parse and check manually using JJWT parser
        val claims = Jwts.parserBuilder().setSigningKey(
                Keys.hmacShaKeyFor(
                    "thisissecretthatismorethan256bits-thisissecretthatismorethan256bits".toByteArray()
                )
            ).build().parseClaimsJws(token).body

        // Then
        assertEquals(userId, claims.subject)
        assertEquals(email, claims["email"])
        assertTrue(claims["roles"].toString().contains("USER"))
        assertTrue(claims["roles"].toString().contains("ADMIN"))
        assertEquals(jwtProperties.issuer, claims.issuer)
        assertEquals(jwtProperties.audience, claims.audience)
    }

    @Test
    fun `token expiration is set correctly`() {
        // Given
        val testExpirations = listOf(
            60000L,      // 1 minute
            3600000L,    // 1 hour
            86400000L    // 1 day
        )

        for (expiration in testExpirations) {
            val testProperties = JwtProperties(
                secretKey = jwtProperties.secretKey,
                expiration = expiration,
                issuer = jwtProperties.issuer,
                audience = jwtProperties.audience
            )

            val tokenProvider = JwtTokenProvider(testProperties)

            val userPrincipal = UserPrincipal(
                externalId = "user-123", email = "user@example.com", roles = setOf("USER")
            )

            val authentication = UsernamePasswordAuthenticationToken(
                userPrincipal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
            )

            // When
            val token = tokenProvider.generateToken(authentication)

            // Parse token to check expiration time
            val claims =
                Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(testProperties.secretKey.toByteArray())).build()
                    .parseClaimsJws(token).body

            // Then
            val issuedAt = claims.issuedAt.time
            val expTime = claims.expiration.time

            // Check that expiration is roughly current time + configured expiration
            // Allow for small processing time differences (within 1 second)
            assertTrue(expTime - issuedAt >= expiration - 1000)
            assertTrue(expTime - issuedAt <= expiration + 1000)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "invalid", "eyJ.not.valid", "token without three parts"])
    fun `validateToken returns false for malformed token variants`(invalidToken: String) {
        // When/Then
        StepVerifier.create(jwtTokenProvider.validateToken(invalidToken)).expectNext(false).verifyComplete()
    }

    @Test
    fun `validateToken returns false for token with missing subject`() {
        // Given
        val token = "invalid.token"

        // When/Then - A token without proper structure should be invalid
        StepVerifier.create(jwtTokenProvider.validateToken(token))
            .expectNext(false)  // Should be invalid without a subject
            .verifyComplete()
    }

    @Test
    fun `validateToken returns false for token with incorrect issuer`() {
        // Given
        val token = "invalid.token"

        // When/Then
        StepVerifier.create(jwtTokenProvider.validateToken(token))
            .expectNext(false)  // Should be invalid with wrong issuer
            .verifyComplete()
    }

    @Test
    fun `validateToken returns false for token with incorrect audience`() {
        // Given
        val token = "invalid.token"

        // When/Then
        StepVerifier.create(jwtTokenProvider.validateToken(token))
            .expectNext(false)  // Should be invalid with wrong audience
            .verifyComplete()
    }

    @Test
    fun `generateToken with multiple roles adds them all to the token`() {
        // Given
        val userId = "user-123"
        val email = "user@example.com"
        val roles = setOf("USER", "ADMIN", "MODERATOR", "TESTER")

        val userPrincipal = UserPrincipal(
            externalId = userId, email = email, roles = roles
        )
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()

        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)

        // When
        val token = jwtTokenProvider.generateToken(authentication)

        // Parse and check manually
        val claims =
            Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray())).build()
                .parseClaimsJws(token).body

        // Then
        // Verify all roles are present
        val tokenRoles = claims["roles"].toString()
        for (role in roles) {
            assertTrue(tokenRoles.contains(role), "Token should contain role: $role")
        }
    }
} 