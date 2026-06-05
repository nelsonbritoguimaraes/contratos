package com.contractops.api.measurement.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.measurement.service.MeasurementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/measurements")
class MeasurementController(
    private val service: MeasurementService
) {

    @GetMapping
    fun list(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val list = service.findByContract(contractId, effectiveTenant)
        return ResponseEntity.ok(list.map {
            mapOf(
                "id" to it.id,
                "period" to it.period,
                "baseValue" to it.baseValue,
                "glosaTotal" to it.glosaTotal,
                "finalAmount" to it.finalAmount,
                "status" to it.status,
                "notes" to it.notes
            )
        })
    }

    @PostMapping("/calculate")
    fun calculate(
        @RequestParam contractId: UUID,
        @RequestParam period: String,   // YYYY-MM-DD
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val localPeriod = LocalDate.parse(period)
        val measurement = service.calculateMeasurement(contractId, localPeriod, effectiveTenant)

        return ResponseEntity.ok(
            mapOf(
                "id" to measurement.id,
                "period" to measurement.period,
                "finalAmount" to measurement.finalAmount,
                "glosaTotal" to measurement.glosaTotal,
                "coverageAdjustment" to measurement.coverageAdjustment,
                "status" to measurement.status,
                "message" to "Medição calculada usando GlosaEngine + Attendance (SPEC §15/16)"
            )
        )
    }

    @PostMapping("/{measurementId}/approve")
    fun approve(
        @PathVariable measurementId: UUID,
        @RequestBody(required = false) body: Map<String, Any>?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val approved = service.approveMeasurement(measurementId, effectiveTenant)

        return ResponseEntity.ok(
            mapOf(
                "id" to approved.id,
                "period" to approved.period,
                "finalAmount" to approved.finalAmount,
                "glosaTotal" to approved.glosaTotal,
                "status" to approved.status,
                "message" to "Medição aprovada. Conta a Receber e NFS-e geradas quando integração financeira disponível.",
                "approvalLevel" to (body?.get("nivel") ?: 1),
                "justificativa" to (body?.get("justificativa") ?: "")
            )
        )
    }
}