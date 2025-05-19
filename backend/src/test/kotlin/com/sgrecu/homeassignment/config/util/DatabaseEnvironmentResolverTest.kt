package com.sgrecu.homeassignment.config.util

import com.sgrecu.homeassignment.config.properties.DatabaseProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.env.Environment
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class DatabaseEnvironmentResolverTest {

    @Mock
    private lateinit var environment: Environment

    @InjectMocks
    private lateinit var resolver: DatabaseEnvironmentResolver

    private val dbProperties = DatabaseProperties(
        h2 = DatabaseProperties.H2Config(databaseName = "testdb"), postgres = DatabaseProperties.PostgresConfig(
            host = "testhost", port = 5432, database = "testdb"
        )
    )

    @Test
    fun `should resolve TEST environment when test profile is active`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("test"))

        // When
        val result = resolver.getActiveEnvironment()

        // Then
        assertEquals(DatabaseEnvironment.TEST, result)
    }

    @Test
    fun `should resolve DEV environment when dev profile is active`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("dev"))

        // When
        val result = resolver.getActiveEnvironment()

        // Then
        assertEquals(DatabaseEnvironment.DEV, result)
    }

    @Test
    fun `should resolve STAGING environment when staging profile is active`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("staging"))

        // When
        val result = resolver.getActiveEnvironment()

        // Then
        assertEquals(DatabaseEnvironment.STAGING, result)
    }

    @Test
    fun `should resolve PROD environment when prod profile is active`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("prod"))

        // When
        val result = resolver.getActiveEnvironment()

        // Then
        assertEquals(DatabaseEnvironment.PROD, result)
    }

    @Test
    fun `should resolve PROD environment when no specific profile is active`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("other"))

        // When
        val result = resolver.getActiveEnvironment()

        // Then
        assertEquals(DatabaseEnvironment.PROD, result)
    }

    @Test
    fun `should create in-memory H2 JDBC URL for test environment`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("test"))

        // When
        val result = resolver.createH2JdbcUrl(dbProperties)

        // Then
        assertEquals("jdbc:h2:mem:testdb", result)
    }

    @Test
    fun `should create file-based H2 JDBC URL for dev environment`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("dev"))

        // When
        val result = resolver.createH2JdbcUrl(dbProperties)

        // Then
        assertEquals("jdbc:h2:file:testdb", result)
    }

    @Test
    fun `should create in-memory H2 R2DBC URL for test environment`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("test"))

        // When
        val result = resolver.createH2R2dbcUrl(dbProperties)

        // Then
        assertEquals("r2dbc:h2:mem:///testdb", result)
    }

    @Test
    fun `should create file-based H2 R2DBC URL for dev environment`() {
        // Given
        `when`(environment.activeProfiles).thenReturn(arrayOf("dev"))

        // When
        val result = resolver.createH2R2dbcUrl(dbProperties)

        // Then
        assertEquals("r2dbc:h2:file:///testdb", result)
    }

    @Test
    fun `should create correct PostgreSQL JDBC URL`() {
        // When
        val result = resolver.createPostgresJdbcUrl(dbProperties)

        // Then
        assertEquals("jdbc:postgresql://testhost:5432/testdb", result)
    }

    @Test
    fun `should create correct PostgreSQL R2DBC URL`() {
        // When
        val result = resolver.createPostgresR2dbcUrl(dbProperties)

        // Then
        assertEquals("r2dbc:postgresql://testhost:5432/testdb", result)
    }
} 