package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Transação Financeira — escrituração real do caixa/tesouraria.
 * Diferente de lançamento contábil (este é o movimento de dinheiro).
 * SPEC §22.3
 */
@Entity
@Table(name = "transacoes_financeiras")
class TransacaoFinanceira(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @Column(name = "conta_bancaria_id", nullable = false)
    var contaBancariaId: UUID,

    @Column(name = "tipo", length = 20, nullable = false)
    var tipo: String,                         // ENTRADA, SAIDA

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "origem_tipo", length = 50)
    var origemTipo: String? = null,           // RECEBIMENTO, PAGAMENTO_FOLHA, NFS_E, RETENCAO, CONCILIACAO, MANUAL

    @Column(name = "origem_id")
    val origemId: UUID? = null,

    @Column(name = "historico", columnDefinition = "TEXT", nullable = false)
    var historico: String,

    @Column(name = "conciliado", nullable = false)
    var conciliado: Boolean = false,

    @Column(name = "conciliacao_id")
    var conciliacaoId: UUID? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "TransacaoFinanceira(id=$id, tipo='$tipo', valor=$valor, data=$data, conciliado=$conciliado)"
}