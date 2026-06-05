package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Recebimento executado (baixa de Conta a Receber).
 * SPEC §22.1
 */
@Entity
@Table(name = "recebimentos")
class Recebimento(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "conta_a_receber_id", nullable = false)
    var contaAReceberId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "conta_bancaria_id")
    var contaBancariaId: UUID? = null,

    @Column(name = "nota_fiscal_id")
    var notaFiscalId: UUID? = null,

    @Column(name = "retencoes_aplicadas", columnDefinition = "JSONB")
    var retencoesAplicadas: String? = null,   // JSON detalhando retenções no recebimento

    @Column(name = "observacao", columnDefinition = "TEXT")
    var observacao: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "Recebimento(id=$id, contaAReceberId=$contaAReceberId, valor=$valor, data=$data)"
}