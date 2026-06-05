package com.contractops.api.common.repository

import com.contractops.api.common.domain.Branch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BranchRepository : JpaRepository<Branch, UUID> {

    fun findByTenantId(tenantId: UUID): List<Branch>

    fun findByTenantIdAndCompanyId(tenantId: UUID, companyId: UUID): List<Branch>
}
