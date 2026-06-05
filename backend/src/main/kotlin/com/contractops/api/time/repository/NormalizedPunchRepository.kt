package com.contractops.api.time.repository

import com.contractops.api.time.domain.NormalizedPunch
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface NormalizedPunchRepository : JpaRepository<NormalizedPunch, UUID> {

    fun findByTenantIdAndEmployeeIdAndPunchTimestampBetween(
        tenantId: UUID,
        employeeId: UUID,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<NormalizedPunch>

    fun deleteByTenantIdAndEmployeeIdAndPunchTimestampBetween(
        tenantId: UUID,
        employeeId: UUID,
        start: LocalDateTime,
        end: LocalDateTime
    )
}
