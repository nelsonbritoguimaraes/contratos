package com.contractops.api.schedule.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "post_schedules")
class PostSchedule(
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

    @Column(name = "shift_template_id")
    var shiftTemplateId: UUID? = null,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null,

    @Column(name = "schedule_type", nullable = false, length = 30)
    var scheduleType: String,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "ACTIVE"
) : AuditEntity(), TenantAware
