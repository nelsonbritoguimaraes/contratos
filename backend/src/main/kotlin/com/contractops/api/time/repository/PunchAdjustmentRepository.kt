package com.contractops.api.time.repository

import com.contractops.api.time.domain.PunchAdjustment
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface PunchAdjustmentRepository : JpaRepository<PunchAdjustment, UUID> {
    fun findByTenantIdAndEmployeeIdAndDate(tenantId: UUID, employeeId: UUID, date: LocalDate): List<PunchAdjustment>
    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<PunchAdjustment>
}
