package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.domain.PayrollRubric
import com.contractops.api.rh.service.PayrollRubricService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/rh/rubrics")
class RubricController(
    private val service: PayrollRubricService
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) activeOnly: Boolean = true,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<PayrollRubricResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val rubrics = if (activeOnly) {
            service.findAllActiveByTenant(effectiveTenant)
        } else {
            service.findAllByTenant(effectiveTenant)
        }

        return ResponseEntity.ok(rubrics.map { PayrollRubricResponse.fromEntity(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<PayrollRubricResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val rubric = service.findById(id, effectiveTenant)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(PayrollRubricResponse.fromEntity(rubric))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateRubricRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<PayrollRubricResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val rubric = PayrollRubric(
            tenantId = effectiveTenant,
            code = request.code,
            description = request.description,
            type = request.type,
            calculationType = request.calculationType,
            fixedValue = request.fixedValue,
            percentage = request.percentage,
            reference = request.reference,
            incidesInss = request.incidesInss,
            incidesFgts = request.incidesFgts,
            incidesIrrf = request.incidesIrrf,
            displayOrder = request.displayOrder
        )

        val created = service.create(rubric)

        return ResponseEntity
            .created(URI.create("/api/rh/rubrics/${created.id}"))
            .body(PayrollRubricResponse.fromEntity(created))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRubricRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<PayrollRubricResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updatedEntity = PayrollRubric(
            tenantId = effectiveTenant,
            code = "", // code não é alterável por PUT
            description = request.description ?: "",
            type = request.type ?: "PROVENTO",
            calculationType = request.calculationType ?: "FIXED",
            fixedValue = request.fixedValue,
            percentage = request.percentage,
            reference = request.reference,
            incidesInss = request.incidesInss ?: false,
            incidesFgts = request.incidesFgts ?: false,
            incidesIrrf = request.incidesIrrf ?: false,
            isActive = request.isActive ?: true,
            displayOrder = request.displayOrder ?: 0
        )

        val updated = service.update(id, effectiveTenant, updatedEntity)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(PayrollRubricResponse.fromEntity(updated))
    }

    @DeleteMapping("/{id}")
    fun deactivate(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Void> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val success = service.deactivate(id, effectiveTenant)
        return if (success) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}