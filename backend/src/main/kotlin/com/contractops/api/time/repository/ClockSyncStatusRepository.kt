package com.contractops.api.time.repository

import com.contractops.api.time.domain.ClockSyncStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ClockSyncStatusRepository : JpaRepository<ClockSyncStatus, UUID> {
    fun findByTenantIdAndDeviceId(tenantId: UUID, deviceId: UUID): ClockSyncStatus?
    fun findByTenantId(tenantId: UUID): List<ClockSyncStatus>
}
