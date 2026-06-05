package com.contractops.api.glosa.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.glosa.service.GlosaEngine
import com.contractops.api.glosa.service.GlosaService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/glosas")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO','SUPERVISOR','FISCAL_INTERNO')")
class GlosaController(
    private val glosaEngine: GlosaEngine,
    private val glosaService: GlosaService
) {

    @PostMapping("/calculate")
    fun calculateGlosas(
        @RequestBody request: CalculateGlosasRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<GlosaResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val glosas = glosaEngine.calculateAndSaveGlosas(
            contractId = request.contractId,
            period = request.period,
            tenantId = effectiveTenant
        )
        return ResponseEntity.ok(glosas.map { toResponse(it) })
    }

    @GetMapping
    fun getGlosas(
        @RequestParam contractId: UUID,
        @RequestParam period: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<GlosaResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val localDate = LocalDate.parse(period)
        val glosas = glosaEngine.getGlosasByPeriod(contractId, localDate, effectiveTenant)
        return ResponseEntity.ok(glosas.map { toResponse(it) })
    }

    @PatchMapping("/{id}")
    fun updateGlosa(
        @PathVariable id: UUID,
        @RequestBody request: UpdateGlosaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val updated = glosaService.updateStatus(
            id = id,
            tenantId = effectiveTenant,
            status = request.status,
            description = request.description,
            evidenceUrl = request.evidenceUrl
        )
        return ResponseEntity.ok(toResponse(updated))
    }

    /** SPEC §26: POST /glosas/{id}/appeal */
    @PostMapping("/{id}/appeal")
    fun appealGlosa(
        @PathVariable id: UUID,
        @RequestBody request: GlosaAppealRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val updated = glosaService.appeal(
            id = id,
            tenantId = effectiveTenant,
            motivo = request.motivo,
            evidenceUrl = request.evidenceUrl
        )
        return ResponseEntity.ok(toResponse(updated))
    }

    private fun toResponse(it: com.contractops.api.glosa.domain.Glosa) = GlosaResponse(
        id = it.id,
        contractId = it.contractId,
        measurementPeriod = it.measurementPeriod,
        glosaType = it.glosaType,
        description = it.description,
        glosaAmount = it.glosaAmount,
        status = it.status
    )
}
