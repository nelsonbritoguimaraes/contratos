package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "bidding_items")
class BiddingItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_lot_id", nullable = false)
    var biddingLot: BiddingLot,

    @Column(name = "codigo_item", length = 50)
    var codigoItem: String? = null,

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    var descricao: String,

    @Column(name = "unidade", length = 20)
    var unidade: String? = null,

    @Column(name = "quantidade", precision = 14, scale = 4)
    var quantidade: BigDecimal = BigDecimal.ONE,

    @Column(name = "valor_unitario", precision = 14, scale = 4)
    var valorUnitario: BigDecimal? = null,

    @Column(name = "valor_total", precision = 16, scale = 2)
    var valorTotal: BigDecimal? = null,

    @Column(name = "tipo", length = 30)
    var tipo: String = "SERVICO"
) : AuditEntity(), TenantAware
