package com.sgrecu.homeassignment.monitoring

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import reactor.test.StepVerifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("AIInfoEndpoint Tests")
class AIInfoEndpointTest {

    private lateinit var applicationContext: ApplicationContext
    private lateinit var environment: Environment
    private lateinit var aiInfoEndpoint: AIInfoEndpoint

    @BeforeEach
    fun setUp() {
        applicationContext = mockk()
        environment = mockk()

        every { environment.getProperty("spring.ai.ollama.chat.options.model") } returns "llama2"
        every { environment.getProperty("spring.ai.ollama.base-url") } returns "http://localhost:11434"
        every { environment.getProperty("spring.ai.ollama.chat.options.temperature") } returns "0.7"
        every { environment.getProperty("spring.application.name") } returns "sgrecu-home-assignment-backend-demo"
        every { environment.activeProfiles } returns arrayOf("dev", "local")

        every { applicationContext.environment } returns environment

        aiInfoEndpoint = AIInfoEndpoint(applicationContext)
    }

    @Test
    fun `info returns correct model information and application info`() {
        // When
        val infoMono = aiInfoEndpoint.info()

        // Then
        StepVerifier.create(infoMono).assertNext { info ->
            val modelInfo = info["model"] as Map<*, *>
            assertEquals("llama2", modelInfo["name"])
            assertEquals("http://localhost:11434", modelInfo["baseUrl"])
            assertEquals("0.7", modelInfo["temperature"])

            val appInfo = info["applicationInfo"] as Map<*, *>
            assertEquals("sgrecu-home-assignment-backend-demo", appInfo["name"])
            assertEquals(listOf("dev", "local"), appInfo["profiles"])

            assertEquals(0, info["activeRequests"])
        }.verifyComplete()
    }

    @Test
    fun `info returns unknown values for missing properties`() {
        // Given
        every { environment.getProperty("spring.ai.ollama.chat.options.model") } returns null
        every { environment.getProperty("spring.ai.ollama.base-url") } returns null
        every { environment.getProperty("spring.application.name") } returns null

        // When
        val infoMono = aiInfoEndpoint.info()

        // Then
        StepVerifier.create(infoMono).assertNext { info ->
            val modelInfo = info["model"] as Map<*, *>
            assertEquals("unknown", modelInfo["name"])
            assertEquals("unknown", modelInfo["baseUrl"])

            val appInfo = info["applicationInfo"] as Map<*, *>
            assertEquals("unknown", appInfo["name"])
        }.verifyComplete()
    }

    @Test
    fun `activeRequests counter increments and decrements correctly`() {
        // When
        aiInfoEndpoint.incrementActiveRequests()
        aiInfoEndpoint.incrementActiveRequests()

        // Then
        StepVerifier.create(aiInfoEndpoint.info()).assertNext { info ->
            assertEquals(2, info["activeRequests"])
        }.verifyComplete()

        // When
        aiInfoEndpoint.decrementActiveRequests()

        // Then
        StepVerifier.create(aiInfoEndpoint.info()).assertNext { info ->
            assertEquals(1, info["activeRequests"])
        }.verifyComplete()
    }

    @Test
    fun `activeRequests counter handles concurrent operations correctly`() {
        val threadCount = 8
        val incrementsPerThread = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val errorTracker = AtomicInteger(0)

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    repeat(incrementsPerThread) {
                        aiInfoEndpoint.incrementActiveRequests()
                    }
                } catch (e: Exception) {
                    errorTracker.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(0, errorTracker.get(), "Concurrent operations should not throw exceptions")

        StepVerifier.create(aiInfoEndpoint.info()).assertNext { info ->
            assertEquals(threadCount * incrementsPerThread, info["activeRequests"])
        }.verifyComplete()
    }
} 