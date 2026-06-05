package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.EmployeeEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface EmployeeEventRepository : JpaRepository<EmployeeEvent, UUID> {

    fun findByTenantIdAndEmployeeIdOrderByEventDateDesc(tenantId: UUID, employeeId: UUID): List<EmployeeEvent>

    fun findByTenantIdAndEmployeeIdAndEventDateBetween(
        tenantId: UUID,
        employeeId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<EmployeeEvent>

    fun findByTenantIdAndEventType(tenantId: UUID, eventType: String): List<EmployeeEvent>
}