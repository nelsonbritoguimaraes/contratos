package com.contractops.api.time.repository

import com.contractops.api.time.domain.PontoEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface PontoEventRepository : JpaRepository<PontoEvent, UUID> {
    fun findByTenantIdAndEmployeeIdAndCompetencia(tenantId: UUID, employeeId: UUID, competencia: LocalDate): List<PontoEvent>
    fun deleteByTenantIdAndEmployeeIdAndCompetencia(tenantId: UUID, employeeId: UUID, competencia: LocalDate)
}
