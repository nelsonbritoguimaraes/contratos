package com.contractops.api.time.repository

import com.contractops.api.time.domain.TimeClockDevice
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TimeClockDeviceRepository : JpaRepository<TimeClockDevice, UUID> {
    fun findByTenantId(tenantId: UUID): List<TimeClockDevice>
}
