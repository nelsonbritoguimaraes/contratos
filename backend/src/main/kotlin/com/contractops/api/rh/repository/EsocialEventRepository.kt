package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.EsocialEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EsocialEventRepository : JpaRepository<EsocialEvent, UUID> {

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<EsocialEvent>

    fun findByTenantIdAndEmployeeId(tenantId: UUID, employeeId: UUID): List<EsocialEvent>
}