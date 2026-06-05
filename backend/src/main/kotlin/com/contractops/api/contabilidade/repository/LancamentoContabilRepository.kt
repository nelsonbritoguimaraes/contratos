package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.LancamentoContabil
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface LancamentoContabilRepository : JpaRepository<LancamentoContabil, UUID> {

    fun findByTenantIdAndDataBetween(tenantId: UUID, start: LocalDate, end: LocalDate): List<LancamentoContabil>

    fun findByTenantIdAndDataBetween(tenantId: UUID, start: LocalDate, end: LocalDate, pageable: Pageable): Page<LancamentoContabil>

    fun findByTenantIdAndOrigemTipoAndOrigemId(tenantId: UUID, origemTipo: String, origemId: UUID): List<LancamentoContabil>

    fun findByTenantIdAndContratoId(tenantId: UUID, contratoId: UUID): List<LancamentoContabil>
}