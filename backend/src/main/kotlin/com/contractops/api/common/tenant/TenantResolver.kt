package com.contractops.api.common.tenant

import org.springframework.stereotype.Component
import java.util.*

/**
 * Responsável por resolver o tenant atual da requisição de forma centralizada.
 *
 * Ordem de precedência:
 * 1. Tenant já definido no TenantContext (ex: vindo do JWT via Security)
 * 2. Header X-Tenant-Id
 * 3. Query param tenantId (modo desenvolvimento)
 *
 * SPEC §4 e §24
 */
@Component
class TenantResolver {

    fun resolveTenantId(headerTenantId: String?, queryTenantId: String?): UUID {
        // 1. Já veio do contexto de segurança (melhor caso)
        TenantContext.getCurrentTenantId()?.let { return it }

        // 2. Header (produção)
        if (!headerTenantId.isNullOrBlank()) {
            return UUID.fromString(headerTenantId)
        }

        // 3. Query param (desenvolvimento)
        if (!queryTenantId.isNullOrBlank()) {
            return UUID.fromString(queryTenantId)
        }

        // Nenhum tenant resolvido — exige tenantId obrigatório
        throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
    }
}
