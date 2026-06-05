package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.FinancialAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FinancialAuditLogRepository : JpaRepository<FinancialAuditLog, UUID> {
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<FinancialAuditLog>
}
