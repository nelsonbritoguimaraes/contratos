package com.contractops.api.compliance.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.compliance.service.ComplianceMonitorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/compliance")
class ComplianceMonitorController(
    private val service: ComplianceMonitorService
) {
    @GetMapping("/monitors")
    fun monitors(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<ComplianceMonitorService.ComplianceMonitor>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listMonitors(t))
    }
}
