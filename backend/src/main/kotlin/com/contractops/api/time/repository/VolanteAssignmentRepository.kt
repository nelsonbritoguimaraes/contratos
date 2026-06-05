package com.contractops.api.time.repository

import com.contractops.api.time.domain.VolanteAssignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface VolanteAssignmentRepository : JpaRepository<VolanteAssignment, UUID> {
    fun findByTenantIdAndContractIdAndAssignmentDate(tenantId: UUID, contractId: UUID, date: LocalDate): List<VolanteAssignment>
    fun findByTenantIdAndContractIdAndAssignmentDateBetween(
        tenantId: UUID,
        contractId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<VolanteAssignment>
    fun findByTenantIdAndWorkflowStatus(tenantId: UUID, status: String): List<VolanteAssignment>
}
