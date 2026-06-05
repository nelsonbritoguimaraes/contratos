package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "bidding_documents")
class BiddingDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "bidding_id", nullable = false)
    val biddingId: UUID,

    @Column(name = "tipo", nullable = false, length = 60)
    var tipo: String,

    @Column(name = "titulo", nullable = false, length = 255)
    var titulo: String,

    @Column(name = "arquivo_nome", length = 255)
    var arquivoNome: String? = null,

    @Column(name = "arquivo_path", length = 500)
    var arquivoPath: String? = null,

    @Column(name = "mime_type", length = 100)
    var mimeType: String? = null,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) : TenantAware
