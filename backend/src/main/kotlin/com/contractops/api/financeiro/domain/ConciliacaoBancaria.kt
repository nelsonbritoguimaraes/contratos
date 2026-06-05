package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Registro de Conciliação Bancária.
 * SPEC §22.3
 */
@Entity
@Table(name = "conciliacoes_bancarias")
class ConciliacaoBancaria(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "conta_bancaria_id", nullable = false)
    var contaBancariaId: UUID,

    @Column(name = "data_inicio", nullable = false)
    var dataInicio: LocalDate,

    @Column(name = "data_fim", nullable = false)
    var dataFim: LocalDate,

    @Column(name = "saldo_extrato", precision = 16, scale = 2, nullable = false)
    var saldoExtrato: BigDecimal,

    @Column(name = "saldo_sistema", precision = 16, scale = 2, nullable = false)
    var saldoSistema: BigDecimal,

    @Column(name = "diferenca", precision = 16, scale = 2, nullable = false)
    var diferenca: BigDecimal,

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "OK",                // OK, DIVERGENTE, EM_ANALISE

    @Column(name = "data_conciliacao", nullable = false)
    var dataConciliacao: LocalDate = LocalDate.now(),

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ConciliacaoBancaria(id=$id, periodo=$dataInicio a $dataFim, status='$status', diferenca=$diferenca)"
}