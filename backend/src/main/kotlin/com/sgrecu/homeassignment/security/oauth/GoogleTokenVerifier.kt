package com.sgrecu.homeassignment.security.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.sgrecu.homeassignment.security.model.UserPrincipal
import com.sgrecu.homeassignment.security.service.UserService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

/**
 * Service responsible for verifying Google OAuth ID tokens and creating user principals.
 * Integrates with the UserService to find or create users based on Google authentication.
 */
@Service
class GoogleTokenVerifier(
    private val properties: GoogleAuthProperties, private val userService: UserService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lazy-initialized Google ID token verifier configured with the application's client ID.
     */
    val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(properties.clientId)).build()
    }

    /**
     * Verifies a Google ID token and creates a UserPrincipal if valid.
     *
     * @param idTokenString The Google ID token to verify
     * @return Mono containing the UserPrincipal if verification succeeds, or empty Mono otherwise
     */
    fun verifyIdToken(idTokenString: String): Mono<UserPrincipal> {
        if (idTokenString.isBlank()) {
            return Mono.empty()
        }

        return Mono.fromCallable {
            try {
                verifier.verify(idTokenString)?.payload
            } catch (e: Exception) {
                logger.error { "Google token verification failed: ${e.message}" }
                null
            }
        }.flatMap { payload ->
            if (payload == null) {
                return@flatMap Mono.empty<UserPrincipal>()
            }

            val userId = payload.subject
            val email = payload["email"] as String? ?: return@flatMap Mono.empty()
            val name = payload["name"] as? String ?: email

            userService.findOrCreateUser(
                externalId = userId, email = email, displayName = name, provider = "google"
            ).map { userWithRoles ->
                UserPrincipal(
                    externalId = userWithRoles.user.externalId,
                    email = userWithRoles.user.email,
                    roles = userWithRoles.roles,
                    attributes = payload
                )
            }
        }.onErrorResume { Mono.empty() }
    }
} 