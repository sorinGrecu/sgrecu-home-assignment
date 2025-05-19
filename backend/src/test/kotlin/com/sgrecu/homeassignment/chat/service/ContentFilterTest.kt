package com.sgrecu.homeassignment.chat.service

import com.sgrecu.homeassignment.chat.config.AIConfig
import com.sgrecu.homeassignment.chat.config.ThinkingConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Duration

@DisplayName("ContentFilter Tests")
class ContentFilterTest {

    private lateinit var aiConfig: AIConfig
    private lateinit var contentFilter: ContentFilter
    private lateinit var thinkingConfig: ThinkingConfig

    @BeforeEach
    fun setup() {
        thinkingConfig = mockk()
        every { thinkingConfig.enabled } returns true
        every { thinkingConfig.startTag } returns "<think>"
        every { thinkingConfig.endTag } returns "</think>"

        aiConfig = mockk()
        every { aiConfig.thinking } returns thinkingConfig

        contentFilter = ContentFilter(aiConfig)
    }

    @Test
    fun `filterContent emits normal tokens unchanged`() {
        // Given
        val tokens = Flux.just("hello", " ", "world")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens, false)).expectNext("hello").expectNext(" ")
            .expectNext("world").verifyComplete()
    }

    @Test
    fun `filterContent_hidesThinkingTags_and_blanks`() {
        // Given
        val tokens = Flux.just("<think>", "internal", "</think>", "visible")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent allows all tokens when thinking mode disabled`() {
        // Given
        val thinkingConfig = mockk<ThinkingConfig>()
        every { thinkingConfig.enabled } returns false
        every { thinkingConfig.startTag } returns "<think>"
        every { thinkingConfig.endTag } returns "</think>"
        every { aiConfig.thinking } returns thinkingConfig

        val disabledFilter = ContentFilter(aiConfig)

        val tokens = Flux.just("<think>", "internal", "</think>", "visible")

        // When/Then
        StepVerifier.create(disabledFilter.filterContent(tokens)).expectNext("<think>").expectNext("internal")
            .expectNext("</think>").expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent removes blanks when configured`() {
        // Given
        val tokens = Flux.just(" ", "\n", "a")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("a").verifyComplete()
    }

    @Test
    fun `filterContent keeps blanks when configured`() {
        // Given
        val tokens = Flux.just(" ", "\n", "a")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens, filterBlanks = false)).expectNext(" ").expectNext("\n")
            .expectNext("a").verifyComplete()
    }

    @Test
    fun `filterContent with complex nested thinking tag patterns`() {
        val tokens = Flux.just("<think>", "</think>", "visible")

        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent with truly nested thinking tags`() {
        val tokens = Flux.just("<think>", "</think>", "visible")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent handles edge cases of blank filtering`() {
        // Given
        val tokens = Flux.just(" ", "\n", "\t", "a")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("a").verifyComplete()
    }

    @Test
    fun `filterContent with multiple consecutive thinking sections`() {
        // Given
        val tokens = Flux.just(
            "<think>", "hidden1", "</think>", "visible1", "<think>", "hidden2", "</think>", "visible2"
        )

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible1").expectNext("visible2")
            .verifyComplete()
    }

    @Test
    fun `filterContent handles empty flux`() {
        // Given
        val tokens = Flux.empty<String>()

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).verifyComplete()
    }

    @Test
    fun `filterContent handles custom thinking tags`() {
        // Given
        val customThinkingConfig = mockk<ThinkingConfig>()
        every { customThinkingConfig.enabled } returns true
        every { customThinkingConfig.startTag } returns "[[thinking]]"
        every { customThinkingConfig.endTag } returns "[[/thinking]]"

        every { aiConfig.thinking } returns customThinkingConfig

        val customFilter = ContentFilter(aiConfig)

        val tokens = Flux.just(
            "[[thinking]]", "internal thought", "[[/thinking]]", "visible output"
        )

        // When/Then
        StepVerifier.create(customFilter.filterContent(tokens)).expectNext("visible output").verifyComplete()
    }

    @Test
    fun `filterContent handles case with both thinking disabled and blank filtering disabled`() {
        // Given
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { disabledThinkingConfig.startTag } returns "<think>"
        every { disabledThinkingConfig.endTag } returns "</think>"

        every { aiConfig.thinking } returns disabledThinkingConfig

        val noFilteringContentFilter = ContentFilter(aiConfig)

        val tokens = Flux.just("<think>", "internal", "</think>", " ", "", "\n", "visible")

        // When/Then
        StepVerifier.create(noFilteringContentFilter.filterContent(tokens, filterBlanks = false)).expectNext("<think>")
            .expectNext("internal").expectNext("</think>").expectNext(" ").expectNext("").expectNext("\n")
            .expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent correctly handles various blank strings`() {
        // Given
        val blanksToFilter = listOf("", " ", "\t", "\n", "\r")

        val nonBlanksToKeep = listOf("a", "hello", "  a", "  ", "aa")

        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig
        val filter = ContentFilter(aiConfig)

        for (blank in blanksToFilter) {
            StepVerifier.create(filter.filterContent(Flux.just(blank))).verifyComplete()
        }

        for (nonBlank in nonBlanksToKeep) {
            StepVerifier.create(filter.filterContent(Flux.just(nonBlank))).expectNext(nonBlank).verifyComplete()
        }
    }

    @Test
    @DisplayName("filterContent should handle non-empty blank strings correctly")
    fun filterContentShouldHandleNonEmptyBlankStringsCorrectly() {
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)

        val singleCharBlankStrings = listOf(" ", "\t", "\n", "\r")

        for (value in singleCharBlankStrings) {
            StepVerifier.create(filter.filterContent(Flux.just(value))).verifyComplete()
        }

        val multiCharBlankStrings = listOf("  ", "   ", "\t\t", "\n\r", "  \t  ")

        for (value in multiCharBlankStrings) {
            StepVerifier.create(filter.filterContent(Flux.just(value))).expectNext(value).verifyComplete()
        }
    }

    @Test
    @DisplayName("filterContent should handle non-blank strings correctly")
    fun filterContentShouldHandleNonBlankStringsCorrectly() {
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)

        val nonBlankStrings = listOf("a", "hello", "  a", "aa")

        for (value in nonBlankStrings) {
            StepVerifier.create(filter.filterContent(Flux.just(value))).expectNext(value).verifyComplete()
        }
    }

    @Test
    @DisplayName("filterContent should handle empty string correctly")
    fun filterContentShouldHandleEmptyStringCorrectly() {
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)

        StepVerifier.create(filter.filterContent(Flux.just(""))).verifyComplete()
    }

    @Test
    @DisplayName("filterContent should handle edge case of spaces with length > 1")
    fun filterContentShouldHandleEdgeCaseOfSpacesWithLengthGreaterThanOne() {
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)

        val tokens = listOf(
            " ", "  ", "   ", "\t", "\t\t", "\n\r", ""
        )

        for (token in tokens) {
            val shouldBeFiltered = token.isBlank() && token.length <= 1
            val filteredFlux = filter.filterContent(Flux.just(token))

            if (shouldBeFiltered) {
                StepVerifier.create(filteredFlux).verifyComplete()
            } else {
                StepVerifier.create(filteredFlux).expectNext(token).verifyComplete()
            }
        }
    }

    @Test
    @DisplayName("filterContent should properly evaluate both branches of the blank filtering condition")
    fun filterContentShouldProperlyEvaluateBothBranchesOfTheBlankFilteringCondition() {
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)

        val case1 = "abc"
        val case2 = "a"
        val case3 = "  "
        val case4 = " "

        val inputFlux = Flux.just(case1, case2, case3, case4)

        // When/Then
        StepVerifier.create(filter.filterContent(inputFlux)).expectNext(case1).expectNext(case2).expectNext(case3)
            .verifyComplete()
    }

    @Test
    fun `filterContent handles multiple thinking tags in complex streams`() {
        // Given
        val tokens = Flux.just(
            "<think>",
            "hidden1",
            "</think>",
            "visible1",
            "<think>",
            "hidden2",
            "still hidden",
            "</think>",
            "visible2",
            "<think>",
            "more hidden",
            "</think>",
            "visible3"
        )

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible1").expectNext("visible2")
            .expectNext("visible3").verifyComplete()
    }

    @Test
    fun `filterContent with incomplete thinking tags keeps content after last end tag`() {
        // Given
        val tokens = Flux.just("<think>", "hidden", "visible")
        val tokens2 = Flux.just("visible1", "<think>", "hidden", "</think>", "visible2")

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).verifyComplete()
        StepVerifier.create(contentFilter.filterContent(tokens2)).expectNext("visible1").expectNext("visible2")
            .verifyComplete()
    }

    @Test
    fun `filterContent handles delayed emissions while preserving order`() {
        // Given
        val tokens =
            Flux.just("<think>", "hidden1", "</think>", "visible1", "<think>", "hidden2", "</think>", "visible2")
                .delayElements(Duration.ofMillis(10))

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible1").expectNext("visible2")
            .verifyComplete()
    }

    @Test
    fun `filterContent correctly handles special characters and unicode`() {
        // Given
        val tokens = Flux.just(
            "<think>", "🔍 hidden emoji", "</think>", "visible 😀", "\u2022 bullet point"
        )

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible 😀")
            .expectNext("\u2022 bullet point").verifyComplete()
    }

    @Test
    fun `filterContent maintains independent state across multiple subscriptions`() {
        // Given
        val tokens = Flux.just("<think>", "hidden", "</think>", "visible")

        // When
        val filteredFlux = contentFilter.filterContent(tokens)

        // Then
        StepVerifier.create(filteredFlux).expectNext("visible").verifyComplete()
        StepVerifier.create(filteredFlux).expectNext("visible").verifyComplete()
    }

    @Test
    fun `filterContent correctly handles backpressure`() {
        // Given
        val tokenCount = 1000
        val tokens = Flux.concat(
            Flux.just("<think>"),
            Flux.range(1, tokenCount / 2).map { "hidden$it" },
            Flux.just("</think>"),
            Flux.range(1, tokenCount / 2).map { "visible$it" })

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNextCount(tokenCount / 2L).verifyComplete()
        StepVerifier.create(contentFilter.filterContent(tokens), 0).thenRequest(1L).expectNextCount(1L).thenRequest(10L)
            .expectNextCount(10L).thenRequest(Long.MAX_VALUE).expectNextCount((tokenCount / 2L) - 11L).verifyComplete()
    }

    @Test
    fun `filterContent handles subscription cancellation properly`() {
        // Given
        val tokens = Flux.concat(
            Flux.just("<think>", "hidden", "</think>", "visible1", "visible2"),
            Flux.interval(Duration.ofMillis(10)).map { "delayed$it" }.take(100)
        )

        // When/Then
        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible1").expectNext("visible2")
            .expectNextCount(5L).thenCancel().verify()
    }

    @Test
    fun `filterContent emits different number of elements than source`() {
        // Given
        val tokens = Flux.just(
            "<think>", "hidden1", "</think>", "visible1", " ", "<think>", "hidden2", "</think>", "visible2"
        )

        StepVerifier.create(contentFilter.filterContent(tokens)).expectNext("visible1").expectNext("visible2")
            .verifyComplete()
    }

    @Test
    fun `filterContent with thinking disabled emits different count when blank filtering enabled`() {
        // Given
        val disabledThinkingConfig = mockk<ThinkingConfig>()
        every { disabledThinkingConfig.enabled } returns false
        every { aiConfig.thinking } returns disabledThinkingConfig

        val filter = ContentFilter(aiConfig)
        val tokens = Flux.just("a", " ", "", "\n", "b", "\t", "c", "  ", "d")

        // When/Then
        StepVerifier.create(filter.filterContent(tokens)).expectNext("a").expectNext("b").expectNext("c")
            .expectNext("  ").expectNext("d").verifyComplete()
        StepVerifier.create(filter.filterContent(tokens, false)).expectNext("a").expectNext(" ").expectNext("")
            .expectNext("\n").expectNext("b").expectNext("\t").expectNext("c").expectNext("  ").expectNext("d")
            .verifyComplete()
    }

} 