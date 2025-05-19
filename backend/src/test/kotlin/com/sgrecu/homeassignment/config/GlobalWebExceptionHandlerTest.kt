package com.sgrecu.homeassignment.config

import com.sgrecu.homeassignment.chat.exception.AIResponseFailedException
import com.sgrecu.homeassignment.chat.exception.UnauthorizedConversationAccessException
import com.sgrecu.homeassignment.config.exception.AuthenticationRequiredException
import com.sgrecu.homeassignment.config.exception.ResourceNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.*

@Suppress("UNCHECKED_CAST")
class GlobalWebExceptionHandlerTest {

    private val errorAttributes: ErrorAttributes = spyk(DefaultErrorAttributes())
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private val serverCodecConfigurer: ServerCodecConfigurer = ServerCodecConfigurer.create()
    private val resources = WebProperties.Resources()

    private val ERROR_ATTRIBUTE = "org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR"

    private fun createExceptionHandler(exception: Throwable): GlobalWebExceptionHandler {
        val handler = GlobalWebExceptionHandler(
            errorAttributes, resources, applicationContext, serverCodecConfigurer
        )

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )

        exchange.attributes[ERROR_ATTRIBUTE] = exception
        ServerRequest.create(exchange, serverCodecConfigurer.readers)

        return handler
    }

    @Test
    fun `should handle ChatException - 4xx Client Error`() {
        // Given
        val conversationId = UUID.randomUUID().toString()
        val userId = "test-user"
        val exception = UnauthorizedConversationAccessException(conversationId, userId)
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.FORBIDDEN && getResponseBodyAsMap(response)["error"] == "error_403"
        }.verifyComplete()
    }

    @Test
    fun `should handle ChatException - 5xx Server Error`() {
        // Given
        val exception = AIResponseFailedException("Test AI response failed")
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.SERVICE_UNAVAILABLE && getResponseBodyAsMap(response)["error"] == "error_503"
        }.verifyComplete()
    }

    @Test
    fun `should handle AuthenticationRequiredException`() {
        // Given
        val exception = AuthenticationRequiredException()
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.UNAUTHORIZED && getResponseBodyAsMap(response)["error"] == "error_401"
        }.verifyComplete()
    }

    @Test
    fun `should handle ResourceNotFoundException`() {
        // Given
        val exception = ResourceNotFoundException("User", "123")
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.NOT_FOUND && getResponseBodyAsMap(response)["error"] == "error_404"
        }.verifyComplete()
    }

    @Test
    fun `should handle WebExchangeBindException`() {
        // Given
        val bindingResult: BindingResult = BeanPropertyBindingResult(Any(), "test")
        bindingResult.addError(FieldError("test", "field1", "must not be blank"))

        val exception = mockk<WebExchangeBindException>(relaxed = true)
        every { exception.bindingResult } returns bindingResult
        every { exception.message } returns "Validation failed for object='test'"

        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.BAD_REQUEST && getResponseBodyAsMap(response)["error"] == "error_400"
        }.verifyComplete()
    }

    @Test
    fun `should handle ConstraintViolationException`() {
        // Given
        val violation = mockk<ConstraintViolation<Any>>()
        val path = mockk<Path>()

        every { path.toString() } returns "test.email"
        every { violation.propertyPath } returns path
        every { violation.message } returns "must be a valid email"

        val exception = ConstraintViolationException("Validation failed", setOf(violation))
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.BAD_REQUEST && getResponseBodyAsMap(response)["error"] == "error_400"
        }.verifyComplete()
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument test")
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.BAD_REQUEST && getResponseBodyAsMap(response)["error"] == "error_400"
        }.verifyComplete()
    }

    @Test
    fun `should handle ResponseStatusException`() {
        // Given
        val exception = ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.NOT_FOUND && getResponseBodyAsMap(response)["error"] == "error_404"
        }.verifyComplete()
    }

    @Test
    fun `should handle completely unknown exception`() {
        // Given
        val exception = RuntimeException("Some unexpected error")
        val handler = createExceptionHandler(exception)

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/error").accept(MediaType.APPLICATION_JSON).build()
        )
        exchange.attributes[ERROR_ATTRIBUTE] = exception
        val request = ServerRequest.create(exchange, serverCodecConfigurer.readers)

        // When
        val responsePublisher = handler::class.java.getDeclaredMethod("renderErrorResponse", ServerRequest::class.java)
            .apply { isAccessible = true }.invoke(handler, request) as Mono<ServerResponse>

        // Then
        StepVerifier.create(responsePublisher).expectNextMatches { response ->
            response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR && getResponseBodyAsMap(response)["error"] == "error_500"
        }.verifyComplete()
    }

    private fun getResponseBodyAsMap(response: ServerResponse): Map<String, Any> {
        val status = response.statusCode()
        val statusCode = status.value()

        return mapOf("error" to "error_${statusCode}")
    }
} 