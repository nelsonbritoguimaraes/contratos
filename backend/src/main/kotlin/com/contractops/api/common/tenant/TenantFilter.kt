package com.contractops.api.common.tenant

import com.contractops.api.common.service.TenantService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * TenantFilter — Extrai e valida o tenant da requisição.
 * Agora delega a resolução para TenantResolver (mais limpo após introdução do Security).
 *
 * SPEC §4 e §24
 */
@Component
@Order(1)
class TenantFilter(
    private val tenantService: TenantService,
    private val tenantResolver: TenantResolver
) : OncePerRequestFilter() {

    companion object {
        const val HEADER_NAME = "X-Tenant-Id"
        const val QUERY_PARAM = "tenantId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HEADER_NAME)
        val queryParam = request.getParameter(QUERY_PARAM)

        val tenantId = tenantResolver.resolveTenantId(header, queryParam)

        // Validação básica de existência do tenant
        if (tenantService.findById(tenantId) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant inválido: $tenantId")
            return
        }

        TenantContext.setTenant(tenantId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
