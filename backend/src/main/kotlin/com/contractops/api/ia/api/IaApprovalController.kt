package com.contractops.api.ia.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.ia.domain.IaApprovalQueueItem
import com.contractops.api.ia.service.IaApprovalQueueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/ia/approvals")
class IaApprovalController(
    private val service: IaApprovalQueueService
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "false") pendingOnly: Boolean
    ): ResponseEntity<List<IaApprovalQueueItem>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(if (pendingOnly) service.listPending(t) else service.listAll(t))
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateApprovalRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<IaApprovalQueueItem> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val item = IaApprovalQueueItem(
            tenantId = t,
            agentName = request.agentName,
            actionType = request.actionType,
            entityType = request.entityType,
            entityId = request.entityId,
            contractId = request.contractId,
            title = request.title,
            summary = request.summary,
            payload = request.payload,
            requestedBy = request.requestedBy ?: "ia-system"
        )
        return ResponseEntity.ok(service.enqueue(item))
    }

    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "gestor") reviewer: String,
        @RequestParam(required = false) notes: String?
    ): ResponseEntity<IaApprovalQueueItem> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.approve(id, t, reviewer, notes))
    }

    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "gestor") reviewer: String,
        @RequestParam(required = false) notes: String?
    ): ResponseEntity<IaApprovalQueueItem> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.reject(id, t, reviewer, notes))
    }
}

data class CreateApprovalRequest(
    val agentName: String,
    val actionType: String,
    val entityType: String? = null,
    val entityId: UUID? = null,
    val contractId: UUID? = null,
    val title: String,
    val summary: String? = null,
    val payload: String? = null,
    val requestedBy: String? = null
)
