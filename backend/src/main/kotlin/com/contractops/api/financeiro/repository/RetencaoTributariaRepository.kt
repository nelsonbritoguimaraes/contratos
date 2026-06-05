package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.RetencaoTributaria
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface RetencaoTributariaRepository : JpaRepository<RetencaoTributaria, UUID> {

    fun findByTenantId(tenantId: UUID): List<RetencaoTributaria>

    fun findByTenantIdAndNotaFiscalId(tenantId: UUID, notaFiscalId: UUID): List<RetencaoTributaria>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<RetencaoTributaria>

    fun findByTenantIdAndDataVencimentoBetween(tenantId: UUID, inicio: LocalDate, fim: LocalDate): List<RetencaoTributaria>
}