package com.contractops.api.contabilidade.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "lancamento_contabil_lines")
class LancamentoContabilLine(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "lancamento_id", nullable = false)
    val lancamentoId: UUID,

    @Column(name = "linha_ordem", nullable = false)
    var linhaOrdem: Int = 1,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false)
    var conta: ContaContabil,

    @Column(name = "natureza_linha", nullable = false, length = 1)
    var naturezaLinha: String,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "historico_linha", length = 255)
    var historicoLinha: String? = null,

    @Column(name = "cost_center_id")
    val costCenterId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
