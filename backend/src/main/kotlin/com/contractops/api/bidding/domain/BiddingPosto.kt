package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "bidding_postos")
class BiddingPosto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_id", nullable = false)
    var bidding: Bidding,

    @Column(name = "bidding_lot_id")
    var biddingLotId: UUID? = null,

    @Column(name = "codigo", length = 50)
    var codigo: String? = null,

    @Column(name = "nome", nullable = false, length = 255)
    var nome: String,

    @Column(name = "funcao", length = 150)
    var funcao: String? = null,

    @Column(name = "cbo", length = 20)
    var cbo: String? = null,

    @Column(name = "escala", length = 100)
    var escala: String? = null,

    @Column(name = "jornada_horas")
    var jornadaHoras: Int? = null,

    @Column(name = "valor_mensal", precision = 14, scale = 2)
    var valorMensal: BigDecimal? = null,

    @Column(name = "local_execucao", length = 255)
    var localExecucao: String? = null,

    @Column(name = "municipio_execucao", length = 150)
    var municipioExecucao: String? = null,

    @Column(name = "quantidade")
    var quantidade: Int = 1
) : AuditEntity(), TenantAware
