package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Fechamento Financeiro Mensal (separado do fechamento contábil).
 * SPEC §22
 */
@Entity
@Table(name = "financeiro_periodos_fechamento")
class FinanceiroPeriodoFechamento(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "data_inicio", nullable = false)
    var dataInicio: LocalDate,

    @Column(name = "data_fim", nullable = false)
    var dataFim: LocalDate,

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "ABERTO",            // ABERTO, FECHADO, REABERTO

    @Column(name = "saldo_caixa_inicial", precision = 16, scale = 2)
    var saldoCaixaInicial: BigDecimal? = null,

    @Column(name = "saldo_caixa_final", precision = 16, scale = 2)
    var saldoCaixaFinal: BigDecimal? = null,

    @Column(name = "total_recebimentos", precision = 16, scale = 2)
    var totalRecebimentos: BigDecimal? = null,

    @Column(name = "total_pagamentos", precision = 16, scale = 2)
    var totalPagamentos: BigDecimal? = null,

    @Column(name = "total_retencoes", precision = 16, scale = 2)
    var totalRetencoes: BigDecimal? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null,

    @Column(name = "fechado_por", length = 100)
    var fechadoPor: String? = null,

    @Column(name = "data_fechamento")
    var dataFechamento: OffsetDateTime? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "FinanceiroPeriodoFechamento(id=$id, periodo=$dataInicio a $dataFim, status='$status')"
}