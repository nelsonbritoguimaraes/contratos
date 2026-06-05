package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "bidding_impugnacoes")
class BiddingImpugnacao(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "bidding_id", nullable = false)
    val biddingId: UUID,

    @Column(name = "tipo", nullable = false, length = 40)
    var tipo: String,

    @Column(name = "protocolo", length = 100)
    var protocolo: String? = null,

    @Column(name = "data_protocolo")
    var dataProtocolo: LocalDate? = null,

    @Column(name = "status", length = 40)
    var status: String = "PROTOCOLADO",

    @Column(name = "argumentos", columnDefinition = "TEXT")
    var argumentos: String? = null,

    @Column(name = "resultado", columnDefinition = "TEXT")
    var resultado: String? = null
) : AuditEntity(), TenantAware

@Entity
@Table(name = "bidding_atas")
class BiddingAta(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "bidding_id", nullable = false)
    val biddingId: UUID,

    @Column(name = "numero_ata", length = 100)
    var numeroAta: String? = null,

    @Column(name = "data_sessao")
    var dataSessao: OffsetDateTime? = null,

    @Column(name = "resumo", columnDefinition = "TEXT")
    var resumo: String? = null,

    @Column(name = "arquivo_path", length = 500)
    var arquivoPath: String? = null,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) : TenantAware
