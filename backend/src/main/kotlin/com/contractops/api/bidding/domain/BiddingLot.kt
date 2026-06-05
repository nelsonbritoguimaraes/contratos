package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Lote de Licitação (BiddingLot)
 */
@Entity
@Table(name = "bidding_lots")
class BiddingLot(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_id", nullable = false)
    var bidding: Bidding,

    @Column(name = "numero_lote", length = 50)
    var numeroLote: String? = null,

    @Column(name = "descricao", columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(name = "quantitativo_postos")
    var quantitativoPostos: Int = 0,

    @Column(name = "valor_mensal", precision = 14, scale = 2)
    var valorMensal: BigDecimal? = null,

    @Column(name = "valor_anual", precision = 16, scale = 2)
    var valorAnual: BigDecimal? = null,

    @Column(name = "valor_global", precision = 16, scale = 2)
    var valorGlobal: BigDecimal? = null,

    @Column(name = "prazo_meses")
    var prazoMeses: Int? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "BiddingLot(id=$id, lote='$numeroLote')"
}
