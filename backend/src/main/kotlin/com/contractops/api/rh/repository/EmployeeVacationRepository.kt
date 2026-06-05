package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.EmployeeVacation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeVacationRepository : JpaRepository<EmployeeVacation, UUID> {
    fun findByTenantIdOrderByStartDateDesc(tenantId: UUID): List<EmployeeVacation>
    fun findByTenantIdAndEmployeeId(tenantId: UUID, employeeId: UUID): List<EmployeeVacation>

    @org.springframework.data.jpa.repository.Query("""
        SELECT v FROM EmployeeVacation v
        WHERE v.tenantId = :tenantId
          AND v.contractId = :contractId
          AND v.startDate <= :date
          AND v.endDate >= :date
          AND v.status IN ('PLANNED', 'APPROVED', 'IN_PROGRESS')
    """)
    fun findActiveOnDate(tenantId: UUID, contractId: UUID, date: java.time.LocalDate): List<EmployeeVacation>
}
