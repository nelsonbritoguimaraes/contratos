package com.contractops.api.common.tenant

import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.util.*

/**
 * TenantContext — Gerencia o tenant atual da requisição.
 *
 * Estratégia:
 * - Usa RequestAttributes (Spring) como fonte primária (melhor para web).
 * - Fallback para ThreadLocal para uso em serviços não-web (jobs, testes, etc.).
 *
 * SPEC: Seções 4 (Multiempresa) e 24 (Segurança / Auditoria).
 */
object TenantContext {

    private const val TENANT_ATTRIBUTE = "CURRENT_TENANT_ID"

    private val threadLocalTenant = ThreadLocal<UUID?>()

    fun setTenant(tenantId: UUID) {
        threadLocalTenant.set(tenantId)
        try {
            RequestContextHolder.currentRequestAttributes()
                .setAttribute(TENANT_ATTRIBUTE, tenantId, RequestAttributes.SCOPE_REQUEST)
        } catch (_: Exception) {
            // Não estamos em contexto de requisição (ex: job, teste unitário)
        }
    }

    fun getCurrentTenantId(): UUID? {
        // 1. Tenta do contexto da requisição (mais confiável em web)
        try {
            val fromRequest = RequestContextHolder.currentRequestAttributes()
                .getAttribute(TENANT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) as? UUID
            if (fromRequest != null) return fromRequest
        } catch (_: Exception) {
            // Sem contexto de requisição
        }

        // 2. Fallback ThreadLocal
        return threadLocalTenant.get()
    }

    fun clear() {
        threadLocalTenant.remove()
        try {
            RequestContextHolder.currentRequestAttributes()
                .removeAttribute(TENANT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
        } catch (_: Exception) {
            // ignora
        }
    }
}
