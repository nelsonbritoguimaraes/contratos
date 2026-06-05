package com.contractops.api.uniforme.repository

import com.contractops.api.uniforme.domain.UniformAllocation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UniformAllocationRepository : JpaRepository<UniformAllocation, UUID> {
    fun findByTenantIdAndEmployeeId(tenantId: UUID, employeeId: UUID): List<UniformAllocation>
}