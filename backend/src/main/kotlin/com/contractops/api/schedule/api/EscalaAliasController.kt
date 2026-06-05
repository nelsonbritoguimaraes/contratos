package com.contractops.api.schedule.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.schedule.service.ScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

// Alias PT-BR: rotas /api/escala espelham /api/schedules
@RestController
@RequestMapping("/api/escala")
class EscalaAliasController(
    private val service: ScheduleService
) {
    @GetMapping("/templates")
    fun templates(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) contractId: UUID?
    ) = ResponseEntity.ok(service.listTemplates(tenant(tenantId), contractId))

    @GetMapping("/posts")
    fun postSchedules(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(service.listPostSchedules(tenant(tenantId), contractId))

    @GetMapping("/rosters")
    fun rosters(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(service.listRosters(tenant(tenantId), contractId))

    @GetMapping("/grid")
    fun grid(
        @RequestParam contractId: UUID,
        @RequestParam start: String,
        @RequestParam end: String,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(
        service.generateScheduleGrid(tenant(tenantId), contractId, LocalDate.parse(start), LocalDate.parse(end))
    )

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
}
