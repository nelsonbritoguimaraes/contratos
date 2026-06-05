package com.contractops.api.cct.repository

import com.contractops.api.cct.domain.Cct
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CctRepository : JpaRepository<Cct, UUID> {
    fun findByTenantId(tenantId: UUID): List<Cct>
}