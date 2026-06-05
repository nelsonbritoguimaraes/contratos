package com.contractops.api.schedule.repository

import com.contractops.api.schedule.domain.ShiftTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ShiftTemplateRepository : JpaRepository<ShiftTemplate, UUID> {
    fun findByTenantIdAndContractIdAndIsActiveTrue(tenantId: UUID, contractId: UUID): List<ShiftTemplate>
    fun findByTenantIdAndIsActiveTrue(tenantId: UUID): List<ShiftTemplate>
}
