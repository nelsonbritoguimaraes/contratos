package com.contractops.api.contract.domain

import com.contractops.api.bidding.domain.BiddingLot
import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Lote dentro do Contrato (ContractLot).
 * Representa os lotes que foram efetivamente contratados/executados.
 */
@Entity
@Table(name = "contract_lots")
class ContractLot(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    var contract: Contract,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_lot_id")
    var originalBiddingLot: BiddingLot? = null,

    @Column(name = "numero_lote", length = 50)
    var numeroLote: String? = null,

    @Column(name = "descricao", columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(name = "quantitativo_postos")
    var quantitativoPostos: Int = 0,

    @Column(name = "valor_mensal", precision = 14, scale = 2)
    var valorMensal: BigDecimal? = null,

    @Column(name = "valor_global", precision = 16, scale = 2)
    var valorGlobal: BigDecimal? = null

) : AuditEntity(), TenantAware
