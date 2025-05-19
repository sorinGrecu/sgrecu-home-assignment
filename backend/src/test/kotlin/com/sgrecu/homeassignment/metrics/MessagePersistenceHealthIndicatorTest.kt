package com.sgrecu.homeassignment.metrics

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.boot.actuate.health.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MessagePersistenceHealthIndicatorTest {

    private val metricsService = mock(MetricsService::class.java)
    private val healthIndicator = MessagePersistenceHealthIndicator(metricsService)

    @Test
    fun `health should return UP status when failure count is below warning threshold`() {
        // Given
        `when`(metricsService.getMessagePersistenceFailureCount()).thenReturn(3L)
        `when`(metricsService.getLostMessagesCount()).thenReturn(2L)

        // When
        val health = healthIndicator.health()

        // Then
        assertEquals(Status.UP, health.status)
        assertEquals(3L, health.details["failures"])
        assertEquals(2L, health.details["lostMessages"])
        assertEquals("OK", health.details["status"])

        verify(metricsService).getMessagePersistenceFailureCount()
        verify(metricsService).getLostMessagesCount()
    }

    @Test
    fun `health should return WARNING status when failure count is between warning and critical thresholds`() {
        // Given
        `when`(metricsService.getMessagePersistenceFailureCount()).thenReturn(10L)
        `when`(metricsService.getLostMessagesCount()).thenReturn(5L)

        // When
        val health = healthIndicator.health()

        // Then
        assertEquals(Status("WARNING"), health.status)
        assertEquals(10L, health.details["failures"])
        assertEquals(5L, health.details["lostMessages"])
        assertEquals("WARNING", health.details["status"])

        verify(metricsService).getMessagePersistenceFailureCount()
        verify(metricsService).getLostMessagesCount()
    }

    @Test
    fun `health should return DOWN status when failure count is at or above critical threshold`() {
        // Given
        `when`(metricsService.getMessagePersistenceFailureCount()).thenReturn(20L)
        `when`(metricsService.getLostMessagesCount()).thenReturn(15L)

        // When
        val health = healthIndicator.health()

        // Then
        assertEquals(Status.DOWN, health.status)
        assertEquals(20L, health.details["failures"])
        assertEquals(15L, health.details["lostMessages"])
        assertEquals("CRITICAL", health.details["status"])

        verify(metricsService).getMessagePersistenceFailureCount()
        verify(metricsService).getLostMessagesCount()
    }

    @Test
    fun `health should return DOWN status when failure count exceeds critical threshold`() {
        // Given
        `when`(metricsService.getMessagePersistenceFailureCount()).thenReturn(30L)
        `when`(metricsService.getLostMessagesCount()).thenReturn(25L)

        // When
        val health = healthIndicator.health()

        // Then
        assertEquals(Status.DOWN, health.status)
        assertEquals(30L, health.details["failures"])
        assertEquals(25L, health.details["lostMessages"])
        assertEquals("CRITICAL", health.details["status"])

        verify(metricsService).getMessagePersistenceFailureCount()
        verify(metricsService).getLostMessagesCount()
    }
} 