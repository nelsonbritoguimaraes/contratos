package com.contractops.api.time.repository

import com.contractops.api.time.domain.AttendanceDay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.*

interface AttendanceDayRepository : JpaRepository<AttendanceDay, UUID> {

    fun findByTenantIdAndEmployeeIdAndDate(
        tenantId: UUID,
        employeeId: UUID,
        date: LocalDate
    ): AttendanceDay?

    fun findByTenantIdAndContractIdAndDate(
        tenantId: UUID,
        contractId: UUID,
        date: LocalDate
    ): List<AttendanceDay>

    @Query("""
        SELECT a FROM AttendanceDay a
        WHERE a.tenantId = :tenantId
          AND a.contractId = :contractId
          AND a.date BETWEEN :start AND :end
    """)
    fun findByTenantIdAndContractIdAndDateBetween(
        tenantId: UUID,
        contractId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<AttendanceDay>

    fun findByTenantIdAndPostIdAndDate(
        tenantId: UUID,
        postId: UUID,
        date: LocalDate
    ): List<AttendanceDay>

    fun findByTenantIdAndEmployeeIdAndDateBetween(
        tenantId: UUID,
        employeeId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<AttendanceDay>
}
