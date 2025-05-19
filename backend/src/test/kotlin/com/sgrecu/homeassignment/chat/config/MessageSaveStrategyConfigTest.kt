package com.sgrecu.homeassignment.chat.config

import com.sgrecu.homeassignment.chat.strategy.MessageSaveStrategy
import com.sgrecu.homeassignment.config.AppProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MessageSaveStrategyConfigTest {

    private lateinit var messageSaveStrategyConfig: MessageSaveStrategyConfig
    private lateinit var appProperties: AppProperties
    private lateinit var strategy1: MessageSaveStrategy
    private lateinit var strategy2: MessageSaveStrategy

    @BeforeEach
    fun setup() {
        messageSaveStrategyConfig = MessageSaveStrategyConfig()
        appProperties = mock()
        strategy1 = mock()
        strategy2 = mock()

        whenever(strategy1.getName()).thenReturn("strategy1")
        whenever(strategy2.getName()).thenReturn("end-of-stream")
    }

    @Test
    fun `should select strategy by name from appProperties`() {
        // Given
        whenever(appProperties.saveStrategy).thenReturn("end-of-stream")
        val strategies = listOf(strategy1, strategy2)

        // When
        val selectedStrategy = messageSaveStrategyConfig.configuredMessageSaveStrategy(strategies, appProperties)

        // Then
        assertSame(strategy2, selectedStrategy)
        verify(strategy1, atLeast(1)).getName()
        verify(strategy2, atLeast(1)).getName()
    }

    @Test
    fun `should use first strategy as default when configured strategy not found`() {
        // Given
        whenever(appProperties.saveStrategy).thenReturn("non-existent-strategy")
        val strategies = listOf(strategy1, strategy2)

        // When
        val selectedStrategy = messageSaveStrategyConfig.configuredMessageSaveStrategy(strategies, appProperties)

        // Then
        assertSame(strategy1, selectedStrategy)
        verify(strategy1, atLeast(1)).getName()
        verify(strategy2, atLeast(1)).getName()
    }

    @Test
    fun `should return empty list when no strategies available`() {
        // Given
        whenever(appProperties.saveStrategy).thenReturn("end-of-stream")
        val strategies = emptyList<MessageSaveStrategy>()

        // When
        val exception = kotlin.runCatching {
            messageSaveStrategyConfig.configuredMessageSaveStrategy(strategies, appProperties)
        }.exceptionOrNull()

        // Then
        assertEquals(NoSuchElementException::class, exception?.let { it::class })
    }
    
    @Test
    fun `should select correct strategy from multiple options`() {
        // Given
        val strategy3 = mock<MessageSaveStrategy>()
        whenever(strategy3.getName()).thenReturn("database")
        whenever(appProperties.saveStrategy).thenReturn("database")
        val strategies = listOf(strategy1, strategy2, strategy3)

        // When
        val selectedStrategy = messageSaveStrategyConfig.configuredMessageSaveStrategy(strategies, appProperties)

        // Then
        assertSame(strategy3, selectedStrategy)
    }
    
    @Test
    fun `should handle case-sensitive strategy names`() {
        // Given
        whenever(appProperties.saveStrategy).thenReturn("Strategy1")
        val strategies = listOf(strategy1, strategy2)

        // When
        val selectedStrategy = messageSaveStrategyConfig.configuredMessageSaveStrategy(strategies, appProperties)

        // Then
        // Should fall back to first strategy since names don't match (case-sensitive)
        assertSame(strategy1, selectedStrategy)
    }
} 