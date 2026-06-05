package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "bidding_deadlines")
class BiddingDeadline(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "bidding_id", nullable = false)
    val biddingId: UUID,

    @Column(name = "tipo", nullable = false, length = 60)
    var tipo: String,

    @Column(name = "descricao", length = 255)
    var descricao: String? = null,

    @Column(name = "data_limite", nullable = false)
    var dataLimite: OffsetDateTime,

    @Column(name = "alerta_dias_antes")
    var alertaDiasAntes: Int = 3,

    @Column(name = "concluido")
    var concluido: Boolean = false,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) : TenantAware
