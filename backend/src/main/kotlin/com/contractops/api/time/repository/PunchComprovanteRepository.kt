package com.contractops.api.time.repository

import com.contractops.api.time.domain.PunchComprovante
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface PunchComprovanteRepository : JpaRepository<PunchComprovante, UUID> {
    fun findByTenantIdAndEmployeeIdAndPunchTimestampAfterOrderByPunchTimestampDesc(
        tenantId: UUID,
        employeeId: UUID,
        since: LocalDateTime
    ): List<PunchComprovante>
}
