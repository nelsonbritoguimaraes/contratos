package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.FinanceiroPeriodoFechamento
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface FinanceiroPeriodoFechamentoRepository : JpaRepository<FinanceiroPeriodoFechamento, UUID> {

    fun findByTenantIdAndDataInicioAndDataFim(
        tenantId: UUID,
        dataInicio: LocalDate,
        dataFim: LocalDate
    ): FinanceiroPeriodoFechamento?

    fun findByTenantIdOrderByDataInicioDesc(tenantId: UUID): List<FinanceiroPeriodoFechamento>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<FinanceiroPeriodoFechamento>
}