package com.contractops.api.common.audit

import com.contractops.api.common.tenant.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val auditService: GlobalAuditService
) {
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','CONTADOR','FINANCEIRO','FISCAL_INTERNO')")
    fun listar(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) entityType: String?
    ): ResponseEntity<List<AuditEvent>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(auditService.listar(effectiveTenant, entityType))
    }
}
