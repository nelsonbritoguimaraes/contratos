package com.contractops.api.schedule.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "employee_rosters")
class EmployeeRoster(
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

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "post_schedule_id")
    var postScheduleId: UUID? = null,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null,

    @Column(name = "role", nullable = false, length = 30)
    var role: String = "TITULAR",

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "ACTIVE"
) : AuditEntity(), TenantAware
