package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.FinanceWorkflow
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FinanceWorkflowRepository : JpaRepository<FinanceWorkflow, UUID> {
    fun findByTenantIdAndConcluidoFalseOrderByCreatedAtDesc(tenantId: UUID): List<FinanceWorkflow>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): FinanceWorkflow?
}
