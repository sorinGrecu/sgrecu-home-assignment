package com.sgrecu.homeassignment.config

import com.sgrecu.homeassignment.chat.exception.ChatException
import com.sgrecu.homeassignment.config.exception.ApplicationException
import jakarta.validation.ConstraintViolationException
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Configuration class for custom error handling
 */
@Configuration
class ErrorHandlingConfig {

    /**
     * Provides the resources configuration needed for the error handler
     */
    @Bean
    fun resources(): WebProperties.Resources {
        return WebProperties.Resources()
    }
}

/**
 * Global WebExceptionHandler for handling all exceptions in a centralized way
 * with proper status codes and response formats.
 */
@Component
@Order(-2)
class GlobalWebExceptionHandler(
    errorAttributes: ErrorAttributes,
    resources: WebProperties.Resources,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(errorAttributes, resources, applicationContext) {

    private val logger = KotlinLogging.logger {}

    init {
        this.setMessageWriters(serverCodecConfigurer.writers)
        this.setMessageReaders(serverCodecConfigurer.readers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
    }

    private fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        return when (val error = getError(request)) {
            is ChatException -> handleChatException(error)
            is ApplicationException -> handleApplicationException(error)
            is WebExchangeBindException -> handleWebExchangeBindException(error)
            is ConstraintViolationException -> handleConstraintViolationException(error)
            is IllegalArgumentException -> {
                logger.warn { "Invalid argument: ${error.message}" }
                createErrorResponse(HttpStatus.BAD_REQUEST, error.message ?: "Invalid argument")
            }

            is ResponseStatusException -> handleResponseStatusException(error)
            else -> handleGenericException(error)
        }
    }

    private fun handleChatException(ex: ChatException): Mono<ServerResponse> {
        when {
            ex.status.is5xxServerError -> logger.error { "Chat service error: ${ex.status} - ${ex.message}" }
            ex.status.is4xxClientError -> logger.warn { "Chat client error: ${ex.status} - ${ex.message}" }
            else -> logger.info { "Chat exception: ${ex.status} - ${ex.message}" }
        }

        return createErrorResponse(ex.status, ex.message)
    }

    private fun handleApplicationException(ex: ApplicationException): Mono<ServerResponse> {
        when {
            ex.status.is5xxServerError -> logger.error { "Application service error: ${ex.status} - ${ex.message}" }
            ex.status.is4xxClientError -> logger.warn { "Application client error: ${ex.status} - ${ex.message}" }
            else -> logger.info { "Application exception: ${ex.status} - ${ex.message}" }
        }

        return createErrorResponse(ex.status, ex.message)
    }

    private fun handleGenericException(error: Throwable): Mono<ServerResponse> {
        logger.error {
            "Application error: ${error.javaClass.simpleName} - ${error.message ?: "No message"}"
        }

        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    private fun handleWebExchangeBindException(ex: WebExchangeBindException): Mono<ServerResponse> {
        logger.warn { "Validation error: ${ex.message}" }

        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        return createValidationErrorResponse(errors)
    }

    private fun handleConstraintViolationException(ex: ConstraintViolationException): Mono<ServerResponse> {
        logger.warn { "Constraint violation: ${ex.message}" }

        val errors = ex.constraintViolations.associate {
            val propertyPath = it.propertyPath.toString()
            val fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
            fieldName to (it.message ?: "Invalid value")
        }

        return createValidationErrorResponse(errors)
    }

    private fun handleResponseStatusException(ex: ResponseStatusException): Mono<ServerResponse> {
        when {
            ex.statusCode.is5xxServerError -> logger.error {
                "Server error: ${ex.statusCode} - ${ex.reason ?: ex.message}"
            }

            ex.statusCode.is4xxClientError -> logger.warn {
                "Client error: ${ex.statusCode} - ${ex.reason ?: ex.message}"
            }

            else -> logger.info { "Status exception: ${ex.statusCode} - ${ex.reason ?: ex.message}" }
        }

        return createErrorResponse(ex.statusCode, ex.reason ?: "Request failed")
    }

    /**
     * Creates a standard error response with the given status and message
     */
    private fun createErrorResponse(
        status: HttpStatusCode, message: String?
    ): Mono<ServerResponse> {
        val errorCode = generateErrorCode(status)

        val responseBody = mapOf(
            "status" to status.value(), "error" to errorCode, "message" to (message ?: "An error occurred")
        )

        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(responseBody))
    }

    /**
     * Generates an error code from the HTTP status
     */
    private fun generateErrorCode(status: HttpStatusCode): String {
        return "error_${status.value()}"
    }

    /**
     * Creates a validation error response with field-specific errors
     */
    private fun createValidationErrorResponse(
        errors: Map<String, String>
    ): Mono<ServerResponse> {
        val responseBody = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to generateErrorCode(HttpStatus.BAD_REQUEST),
            "message" to "Validation failed",
            "errors" to errors
        )

        return ServerResponse.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(responseBody))
    }
} 