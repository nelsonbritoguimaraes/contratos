package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.TransacaoFinanceira
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface TransacaoFinanceiraRepository : JpaRepository<TransacaoFinanceira, UUID> {

    fun findByTenantIdAndDataBetween(tenantId: UUID, inicio: LocalDate, fim: LocalDate): List<TransacaoFinanceira>

    fun findByTenantIdAndContaBancariaId(tenantId: UUID, contaBancariaId: UUID): List<TransacaoFinanceira>

    fun findByTenantIdAndContaBancariaIdAndDataBetween(
        tenantId: UUID,
        contaBancariaId: UUID,
        inicio: LocalDate,
        fim: LocalDate
    ): List<TransacaoFinanceira>

    fun findByTenantIdAndConciliadoFalse(tenantId: UUID): List<TransacaoFinanceira>

    fun findByTenantId(tenantId: UUID): List<TransacaoFinanceira>
}