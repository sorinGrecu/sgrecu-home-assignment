package com.sgrecu.homeassignment.metrics

import com.sgrecu.homeassignment.chat.model.MessageRoleEnum
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@DisplayName("MetricsService Tests")
class MetricsServiceTest {

    private lateinit var meterRegistry: MeterRegistry
    private lateinit var metricsService: MetricsService
    private lateinit var testClock: TestClock

    private val conversationId = UUID.randomUUID()
    private val error = RuntimeException("Test error")

    private val testResetIntervalMs = 100L
    private val initialTimeMs = 1609459200000L

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        testClock = TestClock(initialTimeMs)
        metricsService = MetricsService(meterRegistry, testResetIntervalMs, testClock)
    }

    @Test
    fun `recordMessagePersistenceFailure increments failure counter and gauge`() {
        // When
        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)

        // Then
        val counter = meterRegistry.find("message.persistence.failures").counter()
        val lostMessagesGauge = meterRegistry.find("message.persistence.lost_messages").gauge()
        val recentFailuresGauge = meterRegistry.find("message.persistence.recent_failures").gauge()

        assertTrue(counter != null, "Counter should exist")
        assertTrue(lostMessagesGauge != null, "Lost messages gauge should exist")
        assertTrue(recentFailuresGauge != null, "Recent failures gauge should exist")

        assertEquals(1.0, counter!!.count())
        assertEquals(1.0, lostMessagesGauge!!.value())
        assertEquals(1.0, recentFailuresGauge!!.value())

        val detailedCounter = meterRegistry.find("message.persistence.failure.details").tag("role", "ASSISTANT")
            .tag("error_type", "RuntimeException").tag("error_message", "Test error").counter()

        assertTrue(detailedCounter != null, "Detailed counter should exist")
        assertEquals(1.0, detailedCounter!!.count())
    }

    @Test
    fun `recordMessagePersistenceFailure handles null error message gracefully`() {
        // Given
        val nullMessageError = RuntimeException()

        // When
        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.USER, nullMessageError)

        // Then
        val detailedCounter = meterRegistry.find("message.persistence.failure.details").tag("role", "USER")
            .tag("error_type", "RuntimeException").tag("error_message", "unknown").counter()

        assertTrue(detailedCounter != null, "Detailed counter should exist")
        assertEquals(1.0, detailedCounter!!.count())
    }

    @Test
    fun `recordMessagePersistenceFailure truncates long error messages`() {
        // Given
        val longMessage = "a".repeat(100)
        val longMessageError = RuntimeException(longMessage)

        // When
        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, longMessageError)

        // Then
        val detailedCounter = meterRegistry.find("message.persistence.failure.details").tag("role", "ASSISTANT")
            .tag("error_type", "RuntimeException").tag("error_message", "a".repeat(50)).counter()

        assertTrue(detailedCounter != null, "Detailed counter should exist")
        assertEquals(1.0, detailedCounter!!.count())
    }

    @Test
    fun `multiple failures correctly increment counters and gauges`() {
        // When
        repeat(5) {
            metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)
        }

        // Then
        val counter = meterRegistry.find("message.persistence.failures").counter()
        val lostMessagesGauge = meterRegistry.find("message.persistence.lost_messages").gauge()
        val recentFailuresGauge = meterRegistry.find("message.persistence.recent_failures").gauge()

        assertTrue(counter != null, "Counter should exist")
        assertTrue(lostMessagesGauge != null, "Lost messages gauge should exist")
        assertTrue(recentFailuresGauge != null, "Recent failures gauge should exist")

        assertEquals(5.0, counter!!.count())
        assertEquals(5.0, lostMessagesGauge!!.value())
        assertEquals(5.0, recentFailuresGauge!!.value())
    }

    @Test
    fun `getMessagePersistenceFailureCount returns correct count`() {
        // Given
        repeat(3) {
            metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)
        }

        // When
        val count = metricsService.getMessagePersistenceFailureCount()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `getLostMessagesCount returns correct count`() {
        // Given
        repeat(7) {
            metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)
        }

        // When
        val count = metricsService.getLostMessagesCount()

        // Then
        assertEquals(7, count)
    }

    @Test
    fun `recent failures counter resets after interval`() {
        // Given
        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)
        assertEquals(1, metricsService.getRecentFailuresCount())

        val initialResetTime = metricsService.getLastResetTime()

        // When
        testClock.advanceTimeBy(testResetIntervalMs + 50)

        // Then
        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)

        assertEquals(1, metricsService.getRecentFailuresCount())

        val newResetTime = metricsService.getLastResetTime()
        assertTrue(newResetTime > initialResetTime, "Reset time should be updated")
    }

    @Test
    fun `manual reset works correctly`() {
        // Given
        repeat(5) {
            metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.ASSISTANT, error)
        }

        assertEquals(5, metricsService.getRecentFailuresCount())

        // When
        metricsService.resetRecentFailures()

        // Then
        assertEquals(0, metricsService.getRecentFailuresCount())

        metricsService.recordMessagePersistenceFailure(conversationId, MessageRoleEnum.USER, error)
        assertEquals(1, metricsService.getRecentFailuresCount())
    }

    /**
     * A test implementation of Clock that allows controlling time for testing time-based logic
     */
    private class TestClock(initialTimeMs: Long) : Clock() {
        private val currentTimeMs = AtomicLong(initialTimeMs)

        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(currentTimeMs.get())

        override fun millis(): Long = currentTimeMs.get()

        fun advanceTimeBy(deltaMs: Long) {
            currentTimeMs.addAndGet(deltaMs)
        }

    }
} 