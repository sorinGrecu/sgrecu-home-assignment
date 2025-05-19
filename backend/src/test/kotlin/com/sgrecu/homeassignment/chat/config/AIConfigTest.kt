package com.sgrecu.homeassignment.chat.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIConfigTest {

    @Test
    fun `AIConfig should have default ThinkingConfig`() {
        // When
        val config = AIConfig()
        
        // Then
        assertTrue(config.thinking.enabled)
        assertEquals("<think>", config.thinking.startTag)
        assertEquals("</think>", config.thinking.endTag)
    }
    
    @Test
    fun `AIConfig should accept custom ThinkingConfig`() {
        // Given
        val thinkingConfig = ThinkingConfig(
            enabled = false,
            startTag = "<customThink>",
            endTag = "</customThink>"
        )
        
        // When
        val config = AIConfig(thinking = thinkingConfig)
        
        // Then
        assertEquals(thinkingConfig, config.thinking)
        assertEquals(false, config.thinking.enabled)
        assertEquals("<customThink>", config.thinking.startTag)
        assertEquals("</customThink>", config.thinking.endTag)
    }
    
    @Test
    fun `ThinkingConfig should use default values`() {
        // When
        val config = ThinkingConfig()
        
        // Then
        assertTrue(config.enabled)
        assertEquals("<think>", config.startTag)
        assertEquals("</think>", config.endTag)
    }
    
    @Test
    fun `ThinkingConfig should accept custom values`() {
        // When
        val config = ThinkingConfig(
            enabled = false,
            startTag = "<customTag>",
            endTag = "</customTag>"
        )
        
        // Then
        assertEquals(false, config.enabled)
        assertEquals("<customTag>", config.startTag)
        assertEquals("</customTag>", config.endTag)
    }
} 