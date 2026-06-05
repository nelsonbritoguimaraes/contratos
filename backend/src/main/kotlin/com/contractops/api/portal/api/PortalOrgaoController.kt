package com.contractops.api.portal.api

import com.contractops.api.common.security.AppRole
import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.document.service.DocumentService
import com.contractops.api.measurement.service.MeasurementService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Portal read-only para órgão contratante (ROLE_PORTAL_ORGAO).
 */
@RestController
@RequestMapping("/api/portal/orgao")
@PreAuthorize("hasAnyRole('PORTAL_ORGAO','ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO')")
class PortalOrgaoController(
    private val documentService: DocumentService,
    private val measurementService: MeasurementService
) {
    @GetMapping("/contracts/{contractId}/documents")
    fun documents(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val docs = documentService.findByEntity("CONTRACT", contractId, t)
        return ResponseEntity.ok(docs.map {
            mapOf(
                "id" to it.id,
                "title" to it.title,
                "mimeType" to it.mimeType,
                "status" to it.status,
                "createdAt" to it.createdAt?.toString()
            )
        })
    }

    @GetMapping("/contracts/{contractId}/measurements")
    fun measurements(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val medicoes = measurementService.findByContract(contractId, t)
        return ResponseEntity.ok(medicoes.map {
            mapOf(
                "id" to it.id,
                "period" to it.period.toString(),
                "finalAmount" to it.finalAmount,
                "glosaTotal" to it.glosaTotal,
                "status" to it.status
            )
        })
    }

    @GetMapping("/info")
    fun info(): ResponseEntity<Map<String, Any>> = ResponseEntity.ok(
        mapOf(
            "portal" to "PORTAL_ORGAO",
            "role" to AppRole.PORTAL_ORGAO.name,
            "access" to "read-only"
        )
    )
}
