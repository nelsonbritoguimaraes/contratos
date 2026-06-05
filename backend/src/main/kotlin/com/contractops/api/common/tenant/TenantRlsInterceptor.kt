package com.contractops.api.common.tenant

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Define app.current_tenant na sessão PostgreSQL para RLS (V40).
 */
@Component
class TenantRlsInterceptor(
    private val jdbcTemplate: JdbcTemplate
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val tenantId = TenantContext.getCurrentTenantId()
        if (tenantId != null) {
            // Valida formato UUID rigorosamente antes de interpolar na query
            // UUID.fromString lança IllegalArgumentException se o formato for inválido
            val safeUuid = try { java.util.UUID.fromString(tenantId.toString()).toString() } catch (_: IllegalArgumentException) { return true }
            // SET LOCAL não suporta parâmetros bind no PostgreSQL, mas UUID validado é seguro
            jdbcTemplate.execute("SET LOCAL app.current_tenant = '$safeUuid'")
        }
        return true
    }
}
