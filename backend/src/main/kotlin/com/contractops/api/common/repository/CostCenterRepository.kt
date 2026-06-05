package com.contractops.api.common.repository

import com.contractops.api.common.domain.CostCenter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CostCenterRepository : JpaRepository<CostCenter, UUID> {

    fun findByTenantId(tenantId: UUID): List<CostCenter>

    fun findByTenantIdAndCompanyId(tenantId: UUID, companyId: UUID): List<CostCenter>
}
