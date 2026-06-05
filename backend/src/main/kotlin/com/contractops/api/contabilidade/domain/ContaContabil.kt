package com.contractops.api.contabilidade.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Conta Contábil do Plano de Contas.
 * Suporta estrutura hierárquica (conta mãe).
 */
@Entity
@Table(name = "contas_contabeis")
class ContaContabil(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "codigo", nullable = false, length = 30)
    var codigo: String,   // Ex: 1.1.01.001 ou 3.1.01

    @Column(name = "descricao", nullable = false, length = 200)
    var descricao: String,

    @Column(name = "tipo", nullable = false, length = 20)
    var tipo: String,   // ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA

    @Column(name = "natureza", nullable = false, length = 10)
    var natureza: String,   // DEVEDORA ou CREDORA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_mae_id")
    var contaMae: ContaContabil? = null,

    @Column(name = "nivel", nullable = false)
    var nivel: Int = 1,

    @Column(name = "aceita_lancamento", nullable = false)
    var aceitaLancamento: Boolean = true,

    @Column(name = "ativa", nullable = false)
    var ativa: Boolean = true,

    @Column(name = "codigo_referencial", length = 30)
    var codigoReferencial: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "ContaContabil(codigo='$codigo', descricao='$descricao')"
}