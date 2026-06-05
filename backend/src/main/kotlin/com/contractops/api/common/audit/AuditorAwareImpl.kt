package com.contractops.api.common.audit

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

/**
 * Implementacao de AuditorAware para popular createdBy/updatedBy via JPA auditing.
 * Extrai o username do JWT/SecurityContext ou usa fallback "system".
 */
@Component
class AuditorAwareImpl : AuditorAware<String> {

    override fun getCurrentAuditor(): Optional<String> {
        val auth = SecurityContextHolder.getContext().authentication
        return when {
            auth == null -> Optional.of("system")
            auth.principal is org.springframework.security.oauth2.jwt.Jwt -> {
                val jwt = auth.principal as org.springframework.security.oauth2.jwt.Jwt
                Optional.of(jwt.subject ?: jwt.getClaimAsString("preferred_username") ?: "jwt-user")
            }
            else -> Optional.of(auth.name ?: "authenticated-user")
        }
    }
}
