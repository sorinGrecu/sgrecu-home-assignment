package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.config.AIConfig
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Component for filtering content streams based on configured rules.
 * Centralizes filtering logic like thinking mode tags.
 */
@Component
class ContentFilter(private val aiConfig: AIConfig) {
    /**
     * Filters out thinking mode tags and optionally blanks from a token stream.
     *
     * @param contents The stream of token content to filter
     * @param filterBlanks Whether to filter out blank tokens
     * @return A filtered stream of tokens
     */
    fun filterContent(contents: Flux<String>, filterBlanks: Boolean = true): Flux<String> = Flux.defer {
        val thinkingConfig = aiConfig.thinking
        if (!thinkingConfig.enabled) {
            if (filterBlanks) {
                contents.filter { it.length > 1 || it.isNotBlank() }
            } else {
                contents
            }
        } else {
            contents.scan(Pair(false, "")) { (inThinkingMode, _), token ->
                when (token) {
                    thinkingConfig.startTag -> Pair(true, token)
                    thinkingConfig.endTag -> Pair(false, token)
                    else -> Pair(inThinkingMode, token)
                }
            }.skip(1).filter { (inThinkingMode, token) ->
                !inThinkingMode && token != thinkingConfig.startTag && token != thinkingConfig.endTag
            }.map { it.second }.filter { if (filterBlanks) it.length > 1 || it.isNotBlank() else true }
        }
    }
} 