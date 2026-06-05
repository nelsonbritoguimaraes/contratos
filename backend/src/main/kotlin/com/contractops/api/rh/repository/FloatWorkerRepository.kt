package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.FloatWorker
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FloatWorkerRepository : JpaRepository<FloatWorker, UUID> {
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<FloatWorker>
    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<FloatWorker>
}
