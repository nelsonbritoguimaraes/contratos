package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Planilha Vencedora (WinningSpreadsheet)
 * Fundamental conforme SPEC v1.0 - versionamento de propostas vencedoras.
 */
@Entity
@Table(name = "winning_spreadsheets")
class WinningSpreadsheet(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_id")
    var bidding: Bidding? = null,

    @Column(name = "contract_id")
    var contractId: UUID? = null,

    @Column(name = "versao", nullable = false)
    var versao: Int = 1,

    @Column(name = "arquivo_nome")
    var arquivoNome: String? = null,

    @Column(name = "arquivo_url")
    var arquivoUrl: String? = null,

    @Column(name = "memoria_calculo", columnDefinition = "TEXT")
    var memoriaCalculo: String? = null,

    @Column(name = "is_vencedora", nullable = false)
    var isVencedora: Boolean = false

) : AuditEntity(), TenantAware
