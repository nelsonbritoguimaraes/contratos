package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "float_workers")
class FloatWorker(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "region", length = 100)
    var region: String? = null,

    @Column(name = "enabled_contracts", columnDefinition = "TEXT")
    var enabledContracts: String? = null,

    @Column(name = "enabled_functions", columnDefinition = "TEXT")
    var enabledFunctions: String? = null,

    @Column(name = "availability_notes", columnDefinition = "TEXT")
    var availabilityNotes: String? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "DISPONIVEL",

    @Column(name = "sla_minutes")
    var slaMinutes: Int? = 60
) : AuditEntity(), TenantAware
