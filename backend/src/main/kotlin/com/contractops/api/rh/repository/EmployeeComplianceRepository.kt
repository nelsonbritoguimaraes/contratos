package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.EmployeeCompliance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmployeeComplianceRepository : JpaRepository<EmployeeCompliance, UUID> {
    fun findByTenantIdAndEmployeeIdOrderByDataValidadeDesc(tenantId: UUID, employeeId: UUID): List<EmployeeCompliance>
    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<EmployeeCompliance>
}
