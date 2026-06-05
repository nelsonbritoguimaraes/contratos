package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.AccountingPeriod
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface AccountingPeriodRepository : JpaRepository<AccountingPeriod, UUID> {
    fun findByTenantIdAndCompetencia(tenantId: UUID, competencia: LocalDate): AccountingPeriod?
    fun findByTenantIdOrderByCompetenciaDesc(tenantId: UUID): List<AccountingPeriod>
}
