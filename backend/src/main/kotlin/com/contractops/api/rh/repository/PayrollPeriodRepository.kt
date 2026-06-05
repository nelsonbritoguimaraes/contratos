package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.PayrollPeriod
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface PayrollPeriodRepository : JpaRepository<PayrollPeriod, UUID> {
    fun findByTenantIdOrderByCompetenceDesc(tenantId: UUID): List<PayrollPeriod>
    fun findByTenantIdAndCompetence(tenantId: UUID, competence: LocalDate): List<PayrollPeriod>
}
