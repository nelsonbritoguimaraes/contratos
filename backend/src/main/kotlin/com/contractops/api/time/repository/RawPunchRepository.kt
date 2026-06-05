package com.contractops.api.time.repository

import com.contractops.api.time.domain.RawPunch
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface RawPunchRepository : JpaRepository<RawPunch, UUID> {

    fun findByTenantIdAndPunchTimestampBetween(
        tenantId: UUID,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<RawPunch>

    fun findByDeviceIdAndNsr(deviceId: UUID, nsr: String): RawPunch?
}
