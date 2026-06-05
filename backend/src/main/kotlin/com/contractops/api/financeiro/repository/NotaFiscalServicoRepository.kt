package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.NotaFiscalServico
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface NotaFiscalServicoRepository : JpaRepository<NotaFiscalServico, UUID> {

    fun findByTenantId(tenantId: UUID): List<NotaFiscalServico>

    fun findByTenantIdAndMeasurementId(tenantId: UUID, measurementId: UUID): List<NotaFiscalServico>

    fun findByTenantIdAndContratoId(tenantId: UUID, contratoId: UUID): List<NotaFiscalServico>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<NotaFiscalServico>

    fun findByTenantIdAndDataEmissaoBetween(tenantId: UUID, inicio: LocalDate, fim: LocalDate): List<NotaFiscalServico>

    fun findByTenantIdAndNumeroAndSerie(tenantId: UUID, numero: String, serie: String): NotaFiscalServico?
}