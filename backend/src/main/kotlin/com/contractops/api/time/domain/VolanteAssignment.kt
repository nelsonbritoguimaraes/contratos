package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "volante_assignments")
class VolanteAssignment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "post_id", nullable = false)
    val postId: UUID,

    @Column(name = "absent_employee_id", nullable = false)
    val absentEmployeeId: UUID,

    @Column(name = "volante_employee_id")
    var volanteEmployeeId: UUID? = null,

    @Column(name = "assignment_date", nullable = false)
    val assignmentDate: LocalDate,

    @Column(name = "workflow_status", nullable = false, length = 30)
    var workflowStatus: String = "FALTA_DETECTADA",

    @Column(name = "detected_at")
    var detectedAt: OffsetDateTime? = null,

    @Column(name = "assigned_at")
    var assignedAt: OffsetDateTime? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: OffsetDateTime? = null,

    @Column(name = "evidence_at")
    var evidenceAt: OffsetDateTime? = null,

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    var evidenceUrl: String? = null,

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    var evidenceNotes: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
) : AuditEntity(), TenantAware
