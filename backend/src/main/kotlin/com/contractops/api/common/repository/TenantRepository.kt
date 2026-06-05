package com.contractops.api.common.repository

import com.contractops.api.common.domain.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TenantRepository : JpaRepository<Tenant, UUID> {

    fun findBySlug(slug: String): Tenant?

    fun existsBySlug(slug: String): Boolean
}
