package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Conta a Pagar (AP).
 * Origem: folha, tributos, fornecedores, uniformes, etc.
 * SPEC §22.2
 */
@Entity
@Table(name = "contas_a_pagar")
class ContaAPagar(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "origem", length = 50, nullable = false)
    var origem: String,                       // PAYSLIP, RETENCAO_TRIBUTARIA, FORNECEDOR, UNIFORME, EQUIPAMENTO, SERVICO, MANUAL

    @Column(name = "origem_id")
    val origemId: UUID? = null,

    @Column(name = "contrato_id")
    val contratoId: UUID? = null,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "vencimento", nullable = false)
    var vencimento: LocalDate,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "ABERTO",            // ABERTO, APROVADO, PAGO, VENCIDO, CANCELADO

    @Column(name = "data_pagamento")
    var dataPagamento: LocalDate? = null,

    @Column(name = "valor_pago", precision = 16, scale = 2)
    var valorPago: BigDecimal? = null,

    @Column(name = "forma_pagamento", length = 30)
    var formaPagamento: String? = null,       // PIX, TED, BOLETO, DINHEIRO

    @Column(name = "conta_bancaria_origem_id")
    var contaBancariaOrigemId: UUID? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null,

    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,

    @Column(name = "branch_id")
    var branchId: UUID? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ContaAPagar(id=$id, origem='$origem', valor=$valor, vencimento=$vencimento, status='$status')"
}