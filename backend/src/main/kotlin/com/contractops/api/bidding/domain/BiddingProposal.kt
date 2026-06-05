package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "bidding_proposals")
class BiddingProposal(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_id", nullable = false)
    var bidding: Bidding,

    @Column(name = "versao", nullable = false)
    var versao: Int = 1,

    @Column(name = "cenario", length = 30)
    var cenario: String = "BASE",

    @Column(name = "status", length = 40)
    var status: String = "RASCUNHO",

    @Column(name = "valor_proposta", precision = 16, scale = 2)
    var valorProposta: BigDecimal? = null,

    @Column(name = "margem_estimada_pct", precision = 8, scale = 4)
    var margemEstimadaPct: BigDecimal? = null,

    @Column(name = "custo_total", precision = 16, scale = 2)
    var custoTotal: BigDecimal? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null,

    @Column(name = "tributacao_regime", length = 40)
    var tributacaoRegime: String? = null
) : AuditEntity(), TenantAware
