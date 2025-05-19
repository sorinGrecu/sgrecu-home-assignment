package com.sgrecu.homeassignment.config.exception

import org.springframework.http.HttpStatus

/**
 * Base class for common application exceptions.
 */
sealed class ApplicationException(
    override val message: String, val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when authentication is required for an operation.
 */
class AuthenticationRequiredException(
    message: String = "User authentication required"
) : ApplicationException(message, HttpStatus.UNAUTHORIZED)

/**
 * Exception thrown when a database row cannot be found.
 */
class ResourceNotFoundException(
    resourceType: String, resourceId: String
) : ApplicationException(
    "Resource of type $resourceType with ID $resourceId not found", HttpStatus.NOT_FOUND
) 