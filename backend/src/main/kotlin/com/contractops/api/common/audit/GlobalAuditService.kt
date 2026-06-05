package com.contractops.api.common.audit

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GlobalAuditService(
    private val repository: AuditEventRepository
) {
    @Transactional
    fun registrar(
        tenantId: UUID,
        entityType: String,
        entityId: UUID?,
        action: String,
        actor: String? = "sistema",
        details: String? = null,
        ipAddress: String? = null
    ): AuditEvent = repository.save(
        AuditEvent(
            tenantId = tenantId,
            entityType = entityType,
            entityId = entityId,
            action = action,
            actor = actor,
            details = details,
            ipAddress = ipAddress
        )
    )

    fun listar(tenantId: UUID, entityType: String? = null): List<AuditEvent> =
        if (entityType != null) repository.findByTenantIdAndEntityTypeOrderByOccurredAtDesc(tenantId, entityType)
        else repository.findByTenantIdOrderByOccurredAtDesc(tenantId)
}
