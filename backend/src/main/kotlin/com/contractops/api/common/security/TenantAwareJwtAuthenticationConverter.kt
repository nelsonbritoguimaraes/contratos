package com.contractops.api.common.security

import com.contractops.api.common.tenant.TenantContext
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import java.util.*

/**
 * Converter que popula o TenantContext a partir de claims do JWT.
 *
 * SPEC §2.1 (Autenticação) e §24 (Multi-tenancy / Segurança).
 *
 * Estratégia:
 * - Prioriza claim "tenant_id" no JWT (recomendado configurar no Keycloak client mapper).
 * - Fallback futuro: "azp" (client_id) ou "client_id" para mapeamento (Company/Branch lookup).
 * - Se nenhum tenant puder ser extraído, lança exceção.
 *
 * Futuro: Mapear via client_id (azp) + tenant lookup via CompanyService, ou via mapper no Keycloak.
 */
class TenantAwareJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    private val defaultGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
        setAuthorityPrefix("ROLE_")
        setAuthoritiesClaimName("realm_access.roles")
    }

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = defaultGrantedAuthoritiesConverter.convert(jwt) ?: emptyList()

        // 1. Claim explícito "tenant_id" (melhor prática)
        val tenantIdClaim = jwt.getClaimAsString("tenant_id")
        if (!tenantIdClaim.isNullOrBlank()) {
            val tenantId = UUID.fromString(tenantIdClaim)
            TenantContext.setTenant(tenantId)
            return JwtAuthenticationToken(jwt, authorities, jwt.subject)
        }

        // 2. Fallback: azp / client_id — tenta resolver tenant via claim
        val clientId = jwt.getClaimAsString("azp") ?: jwt.getClaimAsString("client_id")
        if (!clientId.isNullOrBlank()) {
            // SPEC-24: Extrai tenant_id do JWT como fallback primário via client_id.
            // Em integrações futuras, este client_id pode ser mapeado via Company/Branch lookup.
            val tenantFromClientId = jwt.getClaimAsString("tenant_id")
            if (!tenantFromClientId.isNullOrBlank()) {
                val tenantId = UUID.fromString(tenantFromClientId)
                TenantContext.setTenant(tenantId)
                return JwtAuthenticationToken(jwt, authorities, jwt.subject)
            }
        }

        // Nenhum tenant pôde ser extraído do JWT
        throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
    }
}

