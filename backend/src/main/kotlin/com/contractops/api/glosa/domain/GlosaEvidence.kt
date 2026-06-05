package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "glosa_evidences")
class GlosaEvidence(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "glosa_id", nullable = false)
    val glosaId: UUID,

    @Column(name = "evidence_type", length = 50)
    var evidenceType: String? = null,

    @Column(name = "file_url", columnDefinition = "TEXT")
    var fileUrl: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "submitted_by", length = 100)
    var submittedBy: String? = null,

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "PENDENTE"
) : AuditEntity(), TenantAware
