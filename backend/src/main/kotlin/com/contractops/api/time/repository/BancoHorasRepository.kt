package com.contractops.api.time.repository

import com.contractops.api.time.domain.BancoHoras
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface BancoHorasRepository : JpaRepository<BancoHoras, UUID> {
    fun findByTenantIdAndEmployeeIdAndCompetencia(tenantId: UUID, employeeId: UUID, competencia: LocalDate): BancoHoras?
    fun findByTenantIdAndEmployeeIdAndCompetenciaBetween(tenantId: UUID, employeeId: UUID, start: LocalDate, end: LocalDate): List<BancoHoras>
}
