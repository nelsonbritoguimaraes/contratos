package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Conta Bancária / Caixa do tenant.
 * Núcleo da tesouraria (CFO Treasury).
 * SPEC §22.3
 */
@Entity
@Table(name = "contas_bancarias")
class ContaBancaria(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "banco_codigo", nullable = false, length = 10)
    var bancoCodigo: String,           // 001, 033, 104, 237, etc.

    @Column(name = "banco_nome", nullable = false, length = 100)
    var bancoNome: String,

    @Column(name = "agencia", nullable = false, length = 20)
    var agencia: String,

    @Column(name = "conta", nullable = false, length = 30)
    var conta: String,

    @Column(name = "tipo", nullable = false, length = 30)
    var tipo: String = "CORRENTE",     // CORRENTE, POUPANCA, APLICACAO, CAIXA

    @Column(name = "saldo_atual", precision = 16, scale = 2, nullable = false)
    var saldoAtual: BigDecimal = BigDecimal.ZERO,

    @Column(name = "conta_contabil_id")
    var contaContabilId: UUID? = null, // link opcional com plano de contas

    @Column(name = "ativa", nullable = false)
    var ativa: Boolean = true,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ContaBancaria(id=$id, banco=$bancoCodigo, agencia='$agencia', conta='$conta', tipo='$tipo', ativa=$ativa)"
}