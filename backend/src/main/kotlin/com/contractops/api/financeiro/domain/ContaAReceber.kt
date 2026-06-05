package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Conta a Receber (AR).
 * Controla faturamento, vencimentos, retenções e recebimentos.
 * SPEC §22.1 + §16
 */
@Entity
@Table(name = "contas_a_receber")
class ContaAReceber(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contrato_id")
    val contratoId: UUID? = null,

    @Column(name = "measurement_id")
    val measurementId: UUID? = null,

    @Column(name = "nota_fiscal_id")
    var notaFiscalId: UUID? = null,

    @Column(name = "valor_bruto", precision = 16, scale = 2, nullable = false)
    var valorBruto: BigDecimal,

    @Column(name = "valor_liquido", precision = 16, scale = 2, nullable = false)
    var valorLiquido: BigDecimal,

    @Column(name = "vencimento", nullable = false)
    var vencimento: LocalDate,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "ABERTO",          // ABERTO, PARCIAL, PAGO, VENCIDO, CANCELADO

    @Column(name = "dias_atraso", nullable = false)
    var diasAtraso: Int = 0,

    @Column(name = "juros_multa", precision = 16, scale = 2, nullable = false)
    var jurosMulta: BigDecimal = BigDecimal.ZERO,

    @Column(name = "data_recebimento")
    var dataRecebimento: LocalDate? = null,

    @Column(name = "valor_recebido", precision = 16, scale = 2)
    var valorRecebido: BigDecimal? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null,

    @Column(name = "glosa_provisao", precision = 16, scale = 2)
    var glosaProvisao: BigDecimal? = null,

    @Column(name = "tomador_cnpj", length = 18)
    var tomadorCnpj: String? = null,

    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,

    @Column(name = "branch_id")
    var branchId: UUID? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ContaAReceber(id=$id, valorLiquido=$valorLiquido, vencimento=$vencimento, status='$status')"
}