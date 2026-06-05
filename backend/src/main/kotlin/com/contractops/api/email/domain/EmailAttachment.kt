package com.contractops.api.email.domain

import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "email_attachments")
class EmailAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "email_id", nullable = false)
    val emailId: UUID,

    @Column(name = "file_name", length = 255)
    var fileName: String? = null,

    @Column(name = "file_path", length = 500)
    var filePath: String? = null,

    @Column(name = "mime_type", length = 100)
    var mimeType: String? = null,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
) : TenantAware
