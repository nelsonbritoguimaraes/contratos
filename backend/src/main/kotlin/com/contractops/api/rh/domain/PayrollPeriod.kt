package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "payroll_periods")
class PayrollPeriod(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "competence", nullable = false)
    var competence: LocalDate,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "OPEN",

    @Column(name = "closed_at")
    var closedAt: OffsetDateTime? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
) : AuditEntity(), TenantAware
