package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Item de extrato bancário importado (OFX, CSV ou manual).
 * Usado pelo motor de conciliação.
 * SPEC §22.3
 */
@Entity
@Table(name = "extratos_bancarios_itens")
class ExtratoBancarioItem(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "conta_bancaria_id", nullable = false)
    var contaBancariaId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @Column(name = "documento", length = 50)
    var documento: String? = null,

    @Column(name = "historico", columnDefinition = "TEXT", nullable = false)
    var historico: String,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "tipo", length = 10, nullable = false)
    var tipo: String,                         // CREDITO, DEBITO

    @Column(name = "conciliado", nullable = false)
    var conciliado: Boolean = false,

    @Column(name = "transacao_financeira_id")
    var transacaoFinanceiraId: UUID? = null,

    @Column(name = "conciliacao_id")
    var conciliacaoId: UUID? = null,

    @Column(name = "match_confidence", precision = 5, scale = 2)
    var matchConfidence: BigDecimal? = null,

    @Column(name = "nota_fiscal_id")
    var notaFiscalId: UUID? = null,

    @Column(name = "match_metodo", length = 40)
    var matchMetodo: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ExtratoBancarioItem(id=$id, data=$data, valor=$valor, tipo='$tipo', conciliado=$conciliado)"
}