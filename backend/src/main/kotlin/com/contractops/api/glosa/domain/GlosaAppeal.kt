package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "glosa_appeals")
class GlosaAppeal(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "glosa_id", nullable = false)
    val glosaId: UUID,

    @Column(name = "appeal_reason", nullable = false, columnDefinition = "TEXT")
    var appealReason: String,

    @Column(name = "appeal_status", nullable = false, length = 30)
    var appealStatus: String = "ABERTO",

    @Column(name = "submitted_by", length = 100)
    var submittedBy: String? = null,

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "reviewed_by", length = 100)
    var reviewedBy: String? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: OffsetDateTime? = null,

    @Column(name = "review_notes", columnDefinition = "TEXT")
    var reviewNotes: String? = null
) : AuditEntity(), TenantAware
