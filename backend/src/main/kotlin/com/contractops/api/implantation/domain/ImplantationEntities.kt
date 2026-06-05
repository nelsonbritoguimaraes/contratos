package com.contractops.api.implantation.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "contract_implantations")
class ContractImplantation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    var contractId: UUID,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "PLANEJAMENTO",

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "target_date")
    var targetDate: LocalDate? = null,

    @Column(name = "operational_date")
    var operationalDate: LocalDate? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
) : AuditEntity(), TenantAware

@Entity
@Table(name = "implantation_checklist_items")
class ImplantationChecklistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "implantation_id", nullable = false)
    var implantationId: UUID,

    @Column(name = "code", nullable = false, length = 50)
    var code: String,

    @Column(name = "description", nullable = false, length = 300)
    var description: String,

    @Column(name = "required", nullable = false)
    var required: Boolean = true,

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false,

    @Column(name = "completed_at")
    var completedAt: java.time.OffsetDateTime? = null,

    @Column(name = "completed_by", length = 100)
    var completedBy: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
) : AuditEntity(), TenantAware
