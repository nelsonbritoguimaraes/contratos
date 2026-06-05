package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.EmployeeBenefit
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeBenefitRepository : JpaRepository<EmployeeBenefit, UUID> {
    fun findByTenantIdAndIsActiveTrue(tenantId: UUID): List<EmployeeBenefit>
    fun findByTenantIdAndEmployeeId(tenantId: UUID, employeeId: UUID): List<EmployeeBenefit>
}
