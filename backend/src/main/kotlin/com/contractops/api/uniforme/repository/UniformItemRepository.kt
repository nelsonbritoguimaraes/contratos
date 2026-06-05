package com.contractops.api.uniforme.repository

import com.contractops.api.uniforme.domain.UniformItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UniformItemRepository : JpaRepository<UniformItem, UUID> {
    fun findByTenantIdAndActiveTrue(tenantId: UUID): List<UniformItem>
}