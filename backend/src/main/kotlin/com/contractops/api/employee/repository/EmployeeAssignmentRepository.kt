package com.contractops.api.employee.repository

import com.contractops.api.employee.domain.EmployeeAssignment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmployeeAssignmentRepository : JpaRepository<EmployeeAssignment, UUID> {

    fun findByTenantIdAndEmployeeId(tenantId: UUID, employeeId: UUID): List<EmployeeAssignment>

    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<EmployeeAssignment>

    fun findByTenantIdAndPostId(tenantId: UUID, postId: UUID): List<EmployeeAssignment>
}
