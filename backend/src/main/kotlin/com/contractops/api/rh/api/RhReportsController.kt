package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.service.CostByPostReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/rh/reports")
class RhReportsController(
    private val costByPostReportService: CostByPostReportService
) {
    @GetMapping("/custo-por-posto")
    fun custoPorPosto(
        @RequestParam contractId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia).withDayOfMonth(1)
        return ResponseEntity.ok(costByPostReportService.gerarRelatorio(contractId, comp, t))
    }
}
