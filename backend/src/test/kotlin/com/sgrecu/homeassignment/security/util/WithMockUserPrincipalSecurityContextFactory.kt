package com.sgrecu.homeassignment.security.util // Or a suitable test utility package

import com.sgrecu.homeassignment.security.model.UserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory::class)
annotation class WithMockUserPrincipal(
    val externalId: String = "default-test-user-ext-id", // Corresponds to UserPrincipal.getUserId()
    val email: String = "testuser@example.com",
    val roles: Array<String> = ["USER"]
)

class WithMockUserPrincipalSecurityContextFactory : WithSecurityContextFactory<WithMockUserPrincipal> {
    override fun createSecurityContext(annotation: WithMockUserPrincipal): SecurityContext {
        val principal = UserPrincipal(
            externalId = annotation.externalId,
            email = annotation.email,
            roles = annotation.roles.toSet()
        )
        val auth = UsernamePasswordAuthenticationToken(principal, "password", principal.authorities)
        val context = SecurityContextImpl()
        context.authentication = auth
        return context
    }
}