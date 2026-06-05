package com.contractops.api.common.audit

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, UUID> {
    fun findByTenantIdOrderByOccurredAtDesc(tenantId: UUID): List<AuditEvent>
    fun findByTenantIdAndEntityTypeOrderByOccurredAtDesc(tenantId: UUID, entityType: String): List<AuditEvent>
}
