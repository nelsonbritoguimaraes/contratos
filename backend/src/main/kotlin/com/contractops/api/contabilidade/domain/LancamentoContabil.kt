package com.contractops.api.contabilidade.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Lançamento Contábil.
 * Registra movimentação entre contas com rastreabilidade de origem.
 */
@Entity
@Table(name = "lancamentos_contabeis")
class LancamentoContabil(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_debito_id", nullable = false)
    var contaDebito: ContaContabil,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_credito_id", nullable = false)
    var contaCredito: ContaContabil,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "historico", columnDefinition = "TEXT")
    var historico: String? = null,

    @Column(name = "origem_tipo", length = 50)
    var origemTipo: String? = null,   // MEASUREMENT, PAYSLIP, GLOSA, MANUAL, PROVISAO, etc.

    @Column(name = "origem_id")
    val origemId: UUID? = null,   // ID do registro de origem (Payslip, Measurement, etc.)

    @Column(name = "contrato_id")
    val contratoId: UUID? = null,

    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,

    @Column(name = "branch_id")
    var branchId: UUID? = null,

    @Column(name = "composto", nullable = false)
    var composto: Boolean = false

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "LancamentoContabil(id=$id, data=$data, valor=$valor, origem=$origemTipo)"
}