package com.contractops.api.schedule.repository

import com.contractops.api.schedule.domain.EmployeeRoster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface EmployeeRosterRepository : JpaRepository<EmployeeRoster, UUID> {
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<EmployeeRoster>
    fun findByTenantIdAndPostIdAndStatus(tenantId: UUID, postId: UUID, status: String): List<EmployeeRoster>
    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM EmployeeRoster r
        WHERE r.tenantId = :tenantId
          AND r.contractId = :contractId
          AND r.status = 'ACTIVE'
          AND r.effectiveFrom <= :date
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
    """)
    fun findActiveForDate(tenantId: UUID, contractId: UUID, date: LocalDate): List<EmployeeRoster>
}
