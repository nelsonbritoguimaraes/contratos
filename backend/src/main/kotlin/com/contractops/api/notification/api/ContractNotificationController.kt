package com.contractops.api.notification.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.notification.domain.ContractNotification
import com.contractops.api.notification.service.ContractNotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/notificacoes")
class ContractNotificationController(
    private val service: ContractNotificationService
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) contractId: UUID?
    ): ResponseEntity<List<ContractNotification>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.list(t, contractId))
    }

    @PostMapping
    fun create(@RequestBody req: CreateNotificationRequest, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<ContractNotification> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.create(ContractNotification(
            tenantId = t, contractId = req.contractId, notificationNumber = req.notificationNumber,
            orgao = req.orgao, subject = req.subject, description = req.description,
            receivedAt = req.receivedAt, responseDeadline = req.responseDeadline, status = req.status ?: "PENDENTE"
        )))
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody req: Map<String, String>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractNotification> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.updateStatus(id, t, req["status"] ?: "RESPONDIDA"))
    }
}

data class CreateNotificationRequest(
    val contractId: UUID,
    val notificationNumber: String? = null,
    val orgao: String? = null,
    val subject: String,
    val description: String? = null,
    val receivedAt: java.time.LocalDate,
    val responseDeadline: java.time.LocalDate? = null,
    val status: String? = null
)
