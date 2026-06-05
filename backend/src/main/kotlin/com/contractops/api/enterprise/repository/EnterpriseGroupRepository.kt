package com.contractops.api.enterprise.repository

import com.contractops.api.enterprise.domain.EnterpriseGroup
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EnterpriseGroupRepository : JpaRepository<EnterpriseGroup, UUID> {
    fun findByTenantIdOrderByNameAsc(tenantId: UUID): List<EnterpriseGroup>
}
