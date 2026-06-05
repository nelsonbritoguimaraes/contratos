package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.PrevisaoFinanceira
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface PrevisaoFinanceiraRepository : JpaRepository<PrevisaoFinanceira, UUID> {

    fun findByTenantIdAndDataBetween(tenantId: UUID, inicio: LocalDate, fim: LocalDate): List<PrevisaoFinanceira>

    fun findByTenantIdAndCenario(tenantId: UUID, cenario: String): List<PrevisaoFinanceira>
}