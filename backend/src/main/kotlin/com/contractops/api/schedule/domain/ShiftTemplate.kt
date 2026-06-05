package com.contractops.api.schedule.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalTime
import java.util.*

@Entity
@Table(name = "shift_templates")
class ShiftTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "shift_type", nullable = false, length = 30)
    var shiftType: String,

    @Column(name = "work_hours")
    var workHours: Int? = null,

    @Column(name = "rest_hours")
    var restHours: Int? = null,

    @Column(name = "entry_time")
    var entryTime: LocalTime? = null,

    @Column(name = "exit_time")
    var exitTime: LocalTime? = null,

    @Column(name = "cycle_days", nullable = false)
    var cycleDays: Int = 2,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : AuditEntity(), TenantAware
